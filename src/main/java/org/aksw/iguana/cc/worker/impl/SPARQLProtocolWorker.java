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
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
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
                            .POST(HttpRequest.BodyPublishers.ofByteArray(queryStream.readAllBytes())); // InputStream BodyPublisher won't work
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
                            .POST(HttpRequest.BodyPublishers.ofByteArray(queryStream.readAllBytes()));
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
            Optional<HttpResponse<InputStream>> response,
            Instant requestStart,
            Duration duration,
            Optional<BigByteArrayOutputStream> outputStream,
            OptionalLong actualContentLength,
            OptionalLong hash,
            Optional<Exception> exception
    ) {
        public boolean completed() {
            return response.isPresent();
        }

        public boolean successful() {
            if (completed() && exception.isEmpty())
                return (response.get().statusCode() / 100) == 2;
            return false;
        }
    }


    private final HttpClient httpClient;
    private final ExecutorService executor;

    private final XXHashFactory hasherFactory = XXHashFactory.fastestJavaInstance();
    private final RequestFactory requestFactory;

    private final ResponseBodyProcessor responseBodyProcessor;

    // declared here, so it can be reused across multiple method calls
    private BigByteArrayOutputStream responseBodybbaos = new BigByteArrayOutputStream();

    // used to read the http response body
    private byte[] buffer = new byte[4096];


    public Config config() {
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
                        final boolean timeoutBeforeEnd = config().timeout().compareTo(timeToEnd) < 0;
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

        var statusCode = -1;
        if (result.response().isPresent()) {
            statusCode = result.response().get().statusCode();
        }

        if (result.successful()) { // 2xx
            // process result
            if (!responseBodyProcessor.add(result.actualContentLength().getAsLong(), result.hash().getAsLong(), result.outputStream().get())) {
                this.responseBodybbaos = result.outputStream().get();
            } else {
                this.responseBodybbaos = new BigByteArrayOutputStream();
            }
        }

        if (!result.completed() && discardOnFailure) {
            return null;
        }

        return new ExecutionStats(
                result.requestStart(),
                (result.successful()) ? Optional.of(result.duration) : Optional.empty(),
                statusCode,
                result.actualContentLength().orElse(0L),
                result.hash.orElse(0L),
                result.exception().orElse(null)
        );
    }


    private HttpExecutionResult executeHttpRequest(Duration timeout) throws IOException, URISyntaxException {
        final QueryHandler.QueryStreamWrapper queryHandle = config().queries().getNextQueryStream();
        final HttpRequest request = requestFactory.buildHttpRequest(
                queryHandle.queryInputStream(),
                timeout,
                config().connection(),
                config().acceptHeader()
        );

        final Instant requestStart = Instant.now();
        BiFunction<HttpResponse<InputStream>, Exception, HttpExecutionResult> createFailedResult = (response, e) -> {
            final Duration requestDuration = Duration.between(requestStart, Instant.now());
            return new HttpExecutionResult(
                    Optional.ofNullable(response),
                    requestStart,
                    requestDuration,
                    Optional.empty(),
                    OptionalLong.empty(),
                    OptionalLong.empty(),
                    Optional.ofNullable(e)
            );
        };

        try {
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenApply(httpResponse -> {
                        try (final var bodyStream = httpResponse.body()) {
                            if (httpResponse.statusCode() / 100 == 2) { // Request was successful
                                OptionalLong contentLength = httpResponse.headers().firstValueAsLong("Content-Length");
                                try (var hasher = hasherFactory.newStreamingHash64(0)) {
                                    int readBytes;
                                    while ((readBytes = bodyStream.readNBytes(this.buffer, 0, this.buffer.length)) != 0) {
                                        hasher.update(this.buffer, 0, readBytes);
                                        this.responseBodybbaos.write(this.buffer, 0, readBytes);
                                    }

                                    if (contentLength.isPresent() &&
                                            (this.responseBodybbaos.size() < contentLength.getAsLong() ||
                                             this.responseBodybbaos.size() > contentLength.getAsLong())) {
                                        return createFailedResult.apply(httpResponse, null); // TODO: custom exception maybe?
                                    }

                                    return new HttpExecutionResult(
                                            Optional.of(httpResponse),
                                            requestStart,
                                            Duration.between(requestStart, Instant.now()),
                                            Optional.of(this.responseBodybbaos),
                                            OptionalLong.of(this.responseBodybbaos.size()),
                                            OptionalLong.of(hasher.getValue()),
                                            Optional.empty()
                                    );
                                }
                            } else {
                                bodyStream.close();
                                return createFailedResult.apply(httpResponse, null);
                            }
                        } catch (IOException ex) {
                            return createFailedResult.apply(httpResponse, ex);
                        }
                    }).get(timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (CompletionException | InterruptedException | ExecutionException | TimeoutException e) {
            return createFailedResult.apply(null, e);
        }
    }
}
