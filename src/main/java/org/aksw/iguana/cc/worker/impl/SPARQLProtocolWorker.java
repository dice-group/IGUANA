package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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

        public HttpRequest buildHttpRequest(StringBuilder queryBuilder,
                                            String queryID, /* TODO: use for logging? */
                                            Duration timeout,
                                            URI endpoint,
                                            String requestHeader) throws URISyntaxException {
            // TODO: use output stream later to support streaming out huge queries from file
            // TODO: use an int as queryID to reduce the overhead
            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .timeout(timeout)
                    .header("Accept", requestHeader);
            switch (this.requestType) {
                case GET_QUERY -> {
                    request.uri(new URIBuilder(endpoint)
                                    .setParameter("query", queryBuilder.toString())
                                    .build())
                            .GET();
                }
                case POST_URL_ENC_QUERY -> {
                    request.uri(endpoint)
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    urlEncode(Collections.singletonList(
                                            new String[]{"query" /* query is already URL encoded */, queryBuilder.toString()}))));
                }
                case POST_QUERY -> {
                    request.uri(endpoint)
                            .header("Content-Type", "application/sparql-query")
                            .POST(HttpRequest.BodyPublishers.ofString(queryBuilder.toString()));
                }
                case POST_URL_ENC_UPDATE -> {
                    request.uri(endpoint)
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    urlEncode(Collections.singletonList(
                                            new String[]{"update" /* query is already URL encoded */, queryBuilder.toString()}))));
                }
                case POST_UPDATE -> {
                    request.uri(endpoint)
                            .header("Content-Type", "application/sparql-update")
                            .POST(HttpRequest.BodyPublishers.ofString(queryBuilder.toString()));
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

    private final HttpClient httpClient;
    private final ExecutorService executor;
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


    public SPARQLProtocolWorker(HTTPWorker2Task workerTask) {
        this.workerTask = workerTask;
        this.executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        this.requestFactory = new SPARQLProtocolRequestFactory(workerTask.requestType);
        this.httpClient = HttpClient.newBuilder()
                .executor(this.executor)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(workerTask.timeout())
                .build();
    }

    public Future<Void> start() {
        return executor.submit(() -> {
            // do stuff;

            if (workerTask.completionTarget() instanceof QueryMixes queryMixes) {
                for (int i = 0; i < queryMixes.n(); i++) {
                    for (int j = 0; j < workerTask.queryHandler().getQueryCount(); j++) {
                        StringBuilder queryBuilder = new StringBuilder();
                        String queryID = workerTask.queryHandler().getNextQuery(queryBuilder);
                        HttpExecutionResult httpExecutionResult = executeHttpRequest(workerTask.timeout());
                        // TODO: process result and extract relevant infos
                    }

                }
            } else if (workerTask.completionTarget() instanceof TimeLimit timeLimit) {
                final Instant endTime = Instant.now().plus(timeLimit.d());
                Instant now;
                while ((now = Instant.now()).isBefore(endTime)) {
                    final Duration timeToEnd = Duration.between(now, endTime);
                    final boolean timeoutBeforeEnd = workerTask.timeout().compareTo(timeToEnd) > 0;
                    final Duration thisQueryTimeOut = (timeoutBeforeEnd) ? workerTask.timeout() : timeToEnd;
                    HttpExecutionResult httpExecutionResult = executeHttpRequest(thisQueryTimeOut);
                    // TODO: If timed out, decide based on timeoutBeforeEnd if it counts as failed or not
                    // TODO: process result and extract relevant infos
                }

            }
            return null;
        });
    }


    record HttpExecutionResult(HttpResponse<String> response,
                               Instant requestStart,
                               Duration duration,
                               Exception exception) {
    }

    private HttpExecutionResult executeHttpRequest(Duration thisQueryTimeOut) throws IOException, URISyntaxException {
        final StringBuilder queryBuilder = new StringBuilder();
        final String queryID = workerTask.queryHandler().getNextQuery(queryBuilder);
        final HttpRequest request = requestFactory.buildHttpRequest(
                queryBuilder,
                queryID,
                thisQueryTimeOut,
                workerTask.endpoint(),
                workerTask.acceptHeader());
        final Instant requestStart = Instant.now();
        // todo: make body handler configurable to support consuming it as a stream
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            final Duration requestDuration = Duration.between(requestStart, Instant.now());
            return new HttpExecutionResult(null, requestStart, requestDuration, e);
        } catch (InterruptedException e) {
            // TODO: check explicitly for timeouts
            final Duration requestDuration = Duration.between(requestStart, Instant.now());
            return new HttpExecutionResult(null, requestStart, requestDuration, e);
        }
        final Duration requestDuration = Duration.between(requestStart, Instant.now());
        return new HttpExecutionResult(response, requestStart, requestDuration, null);
    }

}
