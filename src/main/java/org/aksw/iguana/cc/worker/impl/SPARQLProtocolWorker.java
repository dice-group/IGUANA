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


    public final static class RequestFactory {
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

        public RequestFactory(RequestType requestType) {
            this.requestType = requestType;
        }

        private static String urlEncode(List<String[]> parameters) {
            return parameters.stream()
                    .map(e -> e[0] + "=" + URLEncoder.encode(e[1], StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
        }

        public HttpRequest buildHttpRequest(InputStream queryStream,
                                            Duration timeout,
                                            ConnectionConfig connection,
                                            String requestHeader) throws URISyntaxException, IOException {
            HttpRequest.Builder request = HttpRequest.newBuilder().timeout(timeout);
            if (requestHeader != null)
                request.header("Accept", requestHeader);
            if (connection.user() != null)
                request.header("Authorization",
                               HttpWorker.basicAuth(connection.user(),
                                                    Optional.ofNullable(connection.password()).orElse("")));
            switch (this.requestType) {
                case GET_QUERY -> {
                    request.uri(new URIBuilder(connection.endpoint())
                                    .setParameter("query",
                                            new String(queryStream.readAllBytes(), StandardCharsets.UTF_8))
                                    .build())
                            .GET();
                }
                case POST_URL_ENC_QUERY -> {
                    request.uri(connection.endpoint())
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    urlEncode(Collections.singletonList(
                                            new String[]{"query" /* query is already URL encoded */,
                                                    new String(queryStream.readAllBytes(), StandardCharsets.UTF_8)}))));
                }
                case POST_QUERY -> {
                    request.uri(connection.endpoint())
                            .header("Content-Type", "application/sparql-query")
                            .POST(HttpRequest.BodyPublishers.ofInputStream(() -> queryStream));
                }
                case POST_URL_ENC_UPDATE -> {
                    request.uri(connection.endpoint())
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    urlEncode(Collections.singletonList(
                                            new String[]{"update" /* query is already URL encoded */,
                                                    new String(queryStream.readAllBytes(), StandardCharsets.UTF_8)}))));
                }
                case POST_UPDATE -> {
                    request.uri(connection.endpoint())
                            .header("Content-Type", "application/sparql-update")
                            .POST(HttpRequest.BodyPublishers.ofInputStream(() -> queryStream));
                }
            }
            return request.build();
        }
    }


    //    @JsonTypeName("SPARQLProtocolWorker")
    public record Config(
            Integer number,
            QueryHandler queries,
            CompletionTarget completionTarget,
            ConnectionConfig connection,
            Duration timeout,
            String acceptHeader /* e.g. application/sparql-results+json */,
            RequestFactory.RequestType requestType,
            boolean parseResults // TODO: integrate this
    ) implements HttpWorker.Config {
        public Config(Integer number,
                      @JsonProperty(required = true) QueryHandler queries,
                      @JsonProperty(required = true) CompletionTarget completionTarget,
                      @JsonProperty(required = true) ConnectionConfig connection,
                      @JsonProperty(required = true) Duration timeout,
                      String acceptHeader,
                      RequestFactory.RequestType requestType,
                      boolean parseResults) {
            this.number = number == null ? 1 : number;
            this.queries = queries;
            this.completionTarget = completionTarget;
            this.connection = connection;
            this.timeout = timeout;
            this.acceptHeader = acceptHeader;
            this.requestType = requestType == null ? RequestFactory.RequestType.GET_QUERY : requestType;
            this.parseResults = parseResults;
        }
    }

    record HttpExecutionResult(
            HttpResponse<InputStream> response,
            Instant requestStart,
            Duration duration,
            BigByteArrayOutputStream outputStream,
            Long actualContentLength,
            Long hash,
            Exception exception
    ) {
        public boolean completed() {
            return response != null;
        }
    }


    private final HttpClient httpClient;
    private final ExecutorService executor;

    private final XXHashFactory hasherFactory = XXHashFactory.fastestJavaInstance();
    private final RequestFactory requestFactory;

    private final ResponseBodyProcessor responseBodyProcessor;

    // declared here, so it can be reused across multiple method calls
    private BigByteArrayOutputStream responseBodybbaos;

    // used to read the http response body
    private byte[] buffer = new byte[4096];


    private Config config() {
        return (Config) config;
    }


    public SPARQLProtocolWorker(long workerId, ResponseBodyProcessor responseBodyProcessor, Config config) {
        super(workerId, responseBodyProcessor, config);
        this.responseBodyProcessor = responseBodyProcessor;
        this.executor = Executors.newFixedThreadPool(2);
        this.requestFactory = new RequestFactory(config().requestType());
        this.httpClient = HttpClient.newBuilder()
                .executor(this.executor)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(config().timeout())
                .build();
    }


    public CompletableFuture<Result> start() {
        return CompletableFuture.supplyAsync(() -> {
            List<ExecutionStats> executionStats = new ArrayList<>();
            try {
                if (config().completionTarget() instanceof QueryMixes queryMixes) {
                    for (int i = 0; i < queryMixes.number(); i++) {
                        for (int j = 0; j < config().queries().getQueryCount(); j++) {
                            ExecutionStats execution = executeQuery(config().timeout(), false);
                            if (execution != null)
                                executionStats.add(execution);
                        }

                    }
                } else if (config().completionTarget() instanceof TimeLimit timeLimit) {
                    final Instant endTime = Instant.now().plus(timeLimit.duration());
                    Instant now;
                    while ((now = Instant.now()).isBefore(endTime)) {
                        final Duration timeToEnd = Duration.between(now, endTime);
                        final boolean timeoutBeforeEnd = config().timeout().compareTo(timeToEnd) > 0;
                        final Duration thisQueryTimeOut = (timeoutBeforeEnd) ? config().timeout() : timeToEnd;
                        // If timeoutBeforeEnd is false, fail shouldn't be counted as timeout
                        ExecutionStats execution = executeQuery(thisQueryTimeOut, !timeoutBeforeEnd);
                        if (execution != null)
                            executionStats.add(execution);
                    }
                }
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }

            return new Result(this.workerId, executionStats);
        }, executor);
    }

    private ExecutionStats executeQuery(Duration timeout, boolean discardOnFailure) throws IOException, URISyntaxException {
        HttpExecutionResult result = executeHttpRequest(timeout);

        int statusCode = -1;
        if (result.completed()) {
            statusCode = result.response().statusCode();
            if (statusCode / 100 == 2) { // 2xx
                // process result
                if (!responseBodyProcessor.add(result.actualContentLength(), result.hash(), result.outputStream())) {
                    this.responseBodybbaos = result.outputStream();
                } else {
                    this.responseBodybbaos = new BigByteArrayOutputStream();
                }
            }
        }

        if (!result.completed() && discardOnFailure) {
            return null;
        }

        return new ExecutionStats(result.requestStart(),
                (result.completed()) ? Optional.of(result.duration) : Optional.empty(),
                statusCode,
                result.actualContentLength(),
                result.hash,
                result.exception()
        );
    }


    private HttpExecutionResult executeHttpRequest(Duration thisQueryTimeOut) throws IOException, URISyntaxException {
        record ExecutionResult(
                HttpResponse<InputStream> httpResponse,
                BigByteArrayOutputStream outputStream,
                Long hash,
                Exception exception
        ) {}

        final QueryHandler.QueryStreamWrapper queryHandle = config().queries().getNextQueryStream();
        final HttpRequest request = requestFactory.buildHttpRequest(
                queryHandle.queryInputStream(),
                thisQueryTimeOut,
                config().connection(),
                config().acceptHeader()
        );

        final Instant requestStart = Instant.now();
        Function<Exception, HttpExecutionResult> httpExecutionResult = (Exception e) -> {
            final Duration requestDuration = Duration.between(requestStart, Instant.now());
            return new HttpExecutionResult(null, requestStart, requestDuration, null, null, null, e);
        };

        ExecutionResult executionResult;
        try {
            executionResult = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenApply(httpResponse -> {
                        try (final var bodyStream = httpResponse.body()) {
                            if (httpResponse.statusCode() / 100 == 2) { // Request was successful
                                OptionalLong contentLength = httpResponse.headers().firstValueAsLong("Content-Length");

                                try (var hasher = hasherFactory.newStreamingHash64(0)) {
                                    int readBytes;
                                    while ((readBytes = bodyStream.readNBytes(this.buffer, 0, this.buffer.length)) != -1) {
                                        hasher.update(this.buffer, 0, readBytes);
                                        this.responseBodybbaos.write(this.buffer, 0, readBytes);
                                    }

                                    if (contentLength.isPresent() && this.responseBodybbaos.size() < contentLength.getAsLong()) {
                                        // TODO: malformed response
                                        return new ExecutionResult(httpResponse, null, null, null);
                                    }
                                    return new ExecutionResult(httpResponse, this.responseBodybbaos, hasher.getValue(), null);
                                }
                            } else {
                                while (bodyStream.readNBytes(this.buffer, 0, this.buffer.length) != -1) {}
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
