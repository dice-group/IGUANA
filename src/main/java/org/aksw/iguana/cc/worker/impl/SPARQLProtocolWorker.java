package org.aksw.iguana.cc.worker.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import net.jpountz.xxhash.XXHashFactory;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.worker.ResponseBodyProcessor;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.commons.io.BigByteArrayOutputStream;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SPARQLProtocolWorker extends HttpWorker {


    public final static class SPARQLProtocolRequestFactory {
        public enum RequestType {
            GET_QUERY("get query"),
            POST_URL_ENC_QUERY("post url-enc query"),
            POST_QUERY("post query"),
            POST_URL_ENC_UPDATE("post url-enc update"),
            POST_UPDATE("post update");

            private final String value;

            @JsonCreator
            RequestType(String value) {
                this.value = Objects.requireNonNullElse(value, "get query");
            }

            @JsonValue
            public String value() {
                return value;
            }
        }

        private final RequestType requestType;

        public SPARQLProtocolRequestFactory(RequestType requestType) {
            this.requestType = requestType;
        }

        private static String urlEncode(List<String[]> parameters) {
            return parameters.stream()
                    .map(e -> e[0] + "=" + URLEncoder.encode(e[1], StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
        }

        public HttpRequest buildHttpRequest(InputStream queryStream,
                                            int queryID, /* TODO: use for logging? */
                                            Duration timeout,
                                            URI endpoint,
                                            String requestHeader) throws URISyntaxException, IOException {
            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .timeout(timeout);
            if (requestHeader != null)
                    request.header("Accept", requestHeader);
            switch (this.requestType) {
                case GET_QUERY -> {
                    request.uri(new URIBuilder(endpoint)
                                    .setParameter("query",
                                            new String(queryStream.readAllBytes(), StandardCharsets.UTF_8))
                                    .build())
                            .GET();
                }
                case POST_URL_ENC_QUERY -> {
                    request.uri(endpoint)
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    urlEncode(Collections.singletonList(
                                            new String[]{"query" /* query is already URL encoded */,
                                                    new String(queryStream.readAllBytes(), StandardCharsets.UTF_8)}))));
                }
                case POST_QUERY -> {
                    request.uri(endpoint)
                            .header("Content-Type", "application/sparql-query")
                            .POST(HttpRequest.BodyPublishers.ofInputStream(() -> queryStream));
                }
                case POST_URL_ENC_UPDATE -> {
                    request.uri(endpoint)
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    urlEncode(Collections.singletonList(
                                            new String[]{"update" /* query is already URL encoded */,
                                                    new String(queryStream.readAllBytes(), StandardCharsets.UTF_8)}))));
                }
                case POST_UPDATE -> {
                    request.uri(endpoint)
                            .header("Content-Type", "application/sparql-update")
                            .POST(HttpRequest.BodyPublishers.ofInputStream(() -> queryStream));
                }
            }
            return request.build();


        }


    }


    private final HttpClient httpClient;
    private final ExecutorService executor;

    private final XXHashFactory hasherFactory = XXHashFactory.fastestJavaInstance();
    private final SPARQLProtocolRequestFactory requestFactory;

    private final ResponseBodyProcessor responseBodyProcessor;

    //    @JsonTypeName("SPARQLProtocolWorker")
    public record Config(Integer number,
                         QueryHandler queries,
                         CompletionTarget completionTarget,
                         ConnectionConfig connection,
                         Duration timeout,
                         String acceptHeader /* e.g. application/sparql-results+json */,
                         SPARQLProtocolRequestFactory.RequestType requestType,
                         boolean parseResults
    ) implements HttpWorker.Config {
        public Config(Integer number,
                      @JsonProperty(required = true) QueryHandler queries,
                      @JsonProperty(required = true) CompletionTarget completionTarget,
                      @JsonProperty(required = true) ConnectionConfig connection,
                      @JsonProperty(required = true) Duration timeout,
                      String acceptHeader,
                      SPARQLProtocolRequestFactory.RequestType requestType,
                      boolean parseResults) {
            this.number = number == null ? 1 : number;
            this.queries = queries;
            this.completionTarget = completionTarget;
            this.connection = connection;
            this.timeout = timeout;
            this.acceptHeader = acceptHeader;
            this.requestType = requestType == null ? SPARQLProtocolRequestFactory.RequestType.GET_QUERY : requestType;
            this.parseResults = parseResults;
        }
    }

    private Config config() {
        return (Config) config;
    }


    public SPARQLProtocolWorker(long workerId, ResponseBodyProcessor responseBodyProcessor, Config config) {
        super(workerId, responseBodyProcessor, config);
        this.responseBodyProcessor = responseBodyProcessor;
        this.executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        this.requestFactory = new SPARQLProtocolRequestFactory(config().requestType());
        this.httpClient = HttpClient.newBuilder()
                .executor(this.executor)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(config().timeout())
                .build();
    }


    public CompletableFuture<Result> start() {
        return CompletableFuture.supplyAsync(() -> {
            List<ExecutionStats> executionStats = new Vector<>();
            try {

                if (config().completionTarget() instanceof QueryMixes queryMixes) {
                    for (int i = 0; i < queryMixes.number(); i++) {
                        for (int j = 0; j < config().queries().getQueryCount(); j++) {
                            HttpExecutionResult httpExecutionResult = executeHttpRequest(config().timeout());

                            // TODO: process result and extract relevant infos
                            // TODO: warp stuff from TimeLimit into a function and use here as well
                        }

                    }
                } else if (config().completionTarget() instanceof TimeLimit timeLimit) {
                    final Instant endTime = Instant.now().plus(timeLimit.duration());
                    Instant now;
                    while ((now = Instant.now()).isBefore(endTime)) {
                        final Duration timeToEnd = Duration.between(now, endTime);
                        final boolean timeoutBeforeEnd = config().timeout().compareTo(timeToEnd) > 0;
                        final Duration thisQueryTimeOut = (timeoutBeforeEnd) ? config().timeout() : timeToEnd;
                        HttpExecutionResult result = executeHttpRequest(thisQueryTimeOut);


                        int statusCode = -1;
                        if (result.completed()) {
                            statusCode = result.response().statusCode();
                            if (statusCode / 100 == 2) { // 2xx
                                // process result
                                boolean bbaosConsumed = responseBodyProcessor.add(result.actualContentLength(), result.hash(), result.outputStream());
                                // TODO: if not bbaosConsumed reset() it and reuse it for the next query.
                            }
                        }
                        // TODO: this should be no checked in code ont in an assertion.
                        assert result.actualContentLength() == result.response().headers().firstValueAsLong("Content-Length").getAsLong();

                        executionStats.add(new ExecutionStats(result.requestStart(),
                                (result.completed()) ? Optional.of(result.duration) : Optional.empty(),
                                statusCode,
                                result.actualContentLength(),
                                result.hash,
                                result.exception()
                        ));
                        // TODO: If timed out, decide based on timeoutBeforeEnd if it counts as failed or not
                    }

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            return new Result(this.workerId, executionStats);
        }, executor);
    }


    record HttpExecutionResult(HttpResponse<InputStream> response,
                               Instant requestStart,
                               Duration duration,
                               BigByteArrayOutputStream outputStream,
                               Long actualContentLength,
                               Long hash,
                               Exception exception) {
        public boolean completed() {
            return response != null;
        }
    }

    private HttpExecutionResult executeHttpRequest(Duration thisQueryTimeOut) throws IOException, URISyntaxException {
        final QueryHandler.QueryStreamWrapper queryHandle = config().queries().getNextQueryStream();
        final HttpRequest request = requestFactory.buildHttpRequest(
                queryHandle.queryInputStream(),
                queryHandle.index(),
                thisQueryTimeOut,
                config().connection().endpoint(), // TODO: we should validate the URI before this point
                config().acceptHeader());
        final Instant requestStart = Instant.now();

        Function<Exception, HttpExecutionResult> httpExecutionResult = (Exception e) -> {
            final Duration requestDuration = Duration.between(requestStart, Instant.now());
            return new HttpExecutionResult(null, requestStart, requestDuration, null, null, null, e);
        };
        record ExecutionResult(HttpResponse<InputStream> httpResponse,
                               BigByteArrayOutputStream outputStream,
                               Long hash,
                               Exception exception) {
        }
        ExecutionResult executionResult;
        try {
            executionResult = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenApply(httpResponse -> {
                        try {
                            final var bodyStream = httpResponse.body();
                            final var buffer = new byte[4096]; // TODO: thread local byte buffer
                            if (httpResponse.statusCode() / 100 == 2) { // Request was successful
                                // TODO: reuse bbaos
                                OptionalLong contentLength = httpResponse.headers().firstValueAsLong("Content-Length");
                                final var bbaos = ((contentLength.isPresent()) ?
                                        new BigByteArrayOutputStream(contentLength.getAsLong()) :
                                        new BigByteArrayOutputStream());

                                try (var hasher = hasherFactory.newStreamingHash64(0)) {
                                    int readBytes;
                                    while ((readBytes = bodyStream.readNBytes(buffer, 0, buffer.length)) != -1) {
                                        hasher.update(buffer, 0, readBytes);
                                        bbaos.write(buffer, 0, readBytes);
                                    }
                                    return new ExecutionResult(httpResponse, bbaos, hasher.getValue(), null);
                                }
                            } else {
                                while (bodyStream.readNBytes(buffer, 0, buffer.length) != -1) {
                                }
                                return new ExecutionResult(httpResponse, null, null, null);
                            }
                        } catch (IOException ex) {
                            return new ExecutionResult(httpResponse, null, null, ex);
                        }
                    }).get(thisQueryTimeOut.getNano(), TimeUnit.NANOSECONDS);
        } catch (CompletionException | InterruptedException | ExecutionException | TimeoutException e) {
            return httpExecutionResult.apply(e);
        }

        final Duration requestDuration = Duration.between(requestStart, Instant.now());
        return new HttpExecutionResult(
                executionResult.httpResponse(),
                requestStart,
                requestDuration,
                executionResult.outputStream(),
                executionResult.outputStream().size(),
                executionResult.hash(),
                executionResult.exception());
    }

}
