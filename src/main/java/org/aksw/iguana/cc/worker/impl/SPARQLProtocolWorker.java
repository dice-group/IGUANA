package org.aksw.iguana.cc.worker.impl;

import net.jpountz.xxhash.XXHashFactory;
import org.aksw.iguana.cc.query.handler.QueryHandler;
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

public class SPARQLProtocolWorker {


    public final static class SPARQLProtocolRequestFactory {
        public enum RequestType {
            GET_QUERY,
            POST_URL_ENC_QUERY,
            POST_QUERY,
            POST_URL_ENC_UPDATE,
            POST_UPDATE
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
                    .timeout(timeout)
                    .header("Accept", requestHeader);
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

    public sealed interface WorkloadCompletionTarget permits TimeLimit, QueryMixes {
    }

    public record TimeLimit(Duration d) implements WorkloadCompletionTarget {
    }

    public record QueryMixes(int n) implements WorkloadCompletionTarget {
    }

    record ExecutionStats(Instant startTime,
                          Optional<Duration> duration,
                          int httpStatusCode,
                          long contentLength,
                          long numberOfBindings,
                          long numberOfSolutions
    ) {
    }

    private final HttpClient httpClient;
    private final int worderId;
    private final ExecutorService executor;

    private final XXHashFactory hasherFactory = XXHashFactory.fastestJavaInstance();
    private final HTTPWorker2Task workerTask;

    private final SPARQLProtocolRequestFactory requestFactory;

    public record HTTPWorker2Task(QueryHandler queryHandler,
                                  WorkloadCompletionTarget completionTarget,
                                  URI endpoint,
                                  Duration timeout,
                                  String acceptHeader /* e.g. application/sparql-results+json */,
                                  SPARQLProtocolRequestFactory.RequestType requestType
    ) {
    }


    public SPARQLProtocolWorker(HTTPWorker2Task workerTask, int workerId) {
        this.workerTask = workerTask;
        this.worderId = workerId;
        this.executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        this.requestFactory = new SPARQLProtocolRequestFactory(workerTask.requestType);
        this.httpClient = HttpClient.newBuilder()
                .executor(this.executor)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(workerTask.timeout())
                .build();
    }

    public record Result(int workerID, List<ExecutionStats> executionStats) {
    }

    public Future<Result> start() {
        return executor.submit(() -> {
            // do stuff;
            List<ExecutionStats> executionStats = new Vector<>();

            if (workerTask.completionTarget() instanceof QueryMixes queryMixes) {
                for (int i = 0; i < queryMixes.n(); i++) {
                    for (int j = 0; j < workerTask.queryHandler().getQueryCount(); j++) {
                        HttpExecutionResult httpExecutionResult = executeHttpRequest(workerTask.timeout());
                        // TODO: process result and extract relevant infos
                        // TODO: warp stuff from TimeLimit into a function and use here as well
                    }

                }
            } else if (workerTask.completionTarget() instanceof TimeLimit timeLimit) {
                final Instant endTime = Instant.now().plus(timeLimit.d());
                Instant now;
                while ((now = Instant.now()).isBefore(endTime)) {
                    final Duration timeToEnd = Duration.between(now, endTime);
                    final boolean timeoutBeforeEnd = workerTask.timeout().compareTo(timeToEnd) > 0;
                    final Duration thisQueryTimeOut = (timeoutBeforeEnd) ? workerTask.timeout() : timeToEnd;
                    HttpExecutionResult result = executeHttpRequest(thisQueryTimeOut);


                    int statusCode = -1;
                    if (result.completed()) {
                        statusCode = result.response().statusCode();
                        if (statusCode / 100 == 2) { // 2xx
                            // process result
                            // TODO: count sparql bindings
                            // TODO: count sparql results
                        }
                    }

                    executionStats.add(new ExecutionStats(result.requestStart(),
                            (result.completed()) ? Optional.of(result.duration) : Optional.empty(),
                            statusCode,
                            result.response().headers().firstValueAsLong("Content-Length").getAsLong(),
                            -1,
                            -1));

                    // TODO: If timed out, decide based on timeoutBeforeEnd if it counts as failed or not
                    // TODO: process result and extract relevant infos
                }

            }
            return new Result(this.worderId, executionStats);
        });
    }


    record HttpExecutionResult(HttpResponse<InputStream> response,
                               Instant requestStart,
                               Duration duration,
                               Exception exception) {
        public boolean completed() {
            return response != null;
        }
    }

    private HttpExecutionResult executeHttpRequest(Duration thisQueryTimeOut) throws IOException, URISyntaxException {
        final QueryHandler.QueryHandle queryHandle = workerTask.queryHandler().getNextQueryStream();
        final HttpRequest request = requestFactory.buildHttpRequest(
                queryHandle.queryInputStream(),
                queryHandle.index(),
                thisQueryTimeOut,
                workerTask.endpoint(),
                workerTask.acceptHeader());
        final Instant requestStart = Instant.now();

        Function<Exception, HttpExecutionResult> httpExecutionResult = (Exception e) -> {
            final Duration requestDuration = Duration.between(requestStart, Instant.now());
            return new HttpExecutionResult(null, requestStart, requestDuration, e);
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
        return new HttpExecutionResult(executionResult.httpResponse(), requestStart, requestDuration, executionResult.exception());
    }

}
