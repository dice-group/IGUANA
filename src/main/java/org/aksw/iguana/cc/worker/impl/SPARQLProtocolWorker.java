package org.aksw.iguana.cc.worker.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import net.jpountz.xxhash.XXHashFactory;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.worker.ResponseBodyProcessor;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.commons.http.HttpClient;
import org.aksw.iguana.commons.http.HttpRequestBuilder;
import org.aksw.iguana.commons.io.BigByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
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

        private static String urlEncode(String name, String value) {
            return name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
        }

        public String buildHttpRequest(InputStream queryStream,
                                                     ConnectionConfig connection,
                                                     String requestHeader) throws URISyntaxException, IOException {
            HttpRequestBuilder requestBuilder = new HttpRequestBuilder();

            final var path = connection.endpoint().getPath();

            switch (this.requestType) {
                case GET_QUERY -> {
                    requestBuilder.setMethod("GET")
                            .setPath(path + "?" + urlEncode("query", new String(queryStream.readAllBytes(), StandardCharsets.UTF_8)));
                }
                case POST_URL_ENC_QUERY -> {
                    // entity will be automatically set to the url encoded parameters
                    requestBuilder.setMethod("POST")
                            .setPath(path)
                            .addHeader("Content-Type", "application/x-www-form-urlencoded");
                }
                case POST_QUERY -> {
                    requestBuilder.setMethod("POST")
                            .setPath(path)
                            .addHeader("Content-Type", "application/sparql-query");
                }
                case POST_URL_ENC_UPDATE -> {
                    requestBuilder.setMethod("POST")
                            .setPath(path)
                            .addHeader("Content-Type", "application/x-www-form-urlencoded");
                }
                case POST_UPDATE -> {
                    requestBuilder.setMethod("POST")
                            .setPath(path)
                            .addHeader("Content-Type", "application/sparql-update");
                }
                default -> throw new IllegalStateException("Unexpected value: " + this.requestType);
            }

            requestBuilder.setProtocol("HTTP/1.1");
            if (requestHeader != null)
                requestBuilder.addHeader("Accept", requestHeader);
            if (connection.authentication() != null && connection.authentication().user() != null)
                requestBuilder.addHeader("Authorization",
                        HttpWorker.basicAuth(connection.authentication().user(),
                                Optional.ofNullable(connection.authentication().password()).orElse("")));

            return requestBuilder.build();
        }
    }


    public record Config(
            Integer number,
            QueryHandler queries,
            CompletionTarget completionTarget,
            ConnectionConfig connection,
            Duration timeout,
            String acceptHeader /* e.g. application/sparql-results+json */,
            RequestFactory.RequestType requestType,
            Boolean parseResults
    ) implements HttpWorker.Config {
        public Config(Integer number,
                      @JsonProperty(required = true) QueryHandler queries,
                      @JsonProperty(required = true) CompletionTarget completionTarget,
                      @JsonProperty(required = true) ConnectionConfig connection,
                      @JsonProperty(required = true) Duration timeout,
                      String acceptHeader,
                      RequestFactory.RequestType requestType,
                      Boolean parseResults) {
            this.number = number == null ? 1 : number;
            this.queries = queries;
            this.completionTarget = completionTarget;
            this.connection = connection;
            this.timeout = timeout;
            this.acceptHeader = acceptHeader;
            this.requestType = requestType == null ? RequestFactory.RequestType.GET_QUERY : requestType;
            this.parseResults = parseResults == null || parseResults;
        }
    }

    record HttpExecutionResult(
            int queryID,
            Optional<?> response,
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
            return true; // TODO:
        }
    }

    private HttpClient httpClient;
    private final ThreadPoolExecutor executor;

    private final XXHashFactory hasherFactory = XXHashFactory.fastestJavaInstance();
    private final RequestFactory requestFactory;

    private final ResponseBodyProcessor responseBodyProcessor;

    // declared here, so it can be reused across multiple method calls
    private BigByteArrayOutputStream responseBodybbaos = new BigByteArrayOutputStream();

    // used to read the http response body
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private static final int BUFFER_SIZE = 4096;

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLProtocolWorker.class);

    @Override
    public Config config() {
        return (SPARQLProtocolWorker.Config) config;
    }

    public SPARQLProtocolWorker(long workerId, ResponseBodyProcessor responseBodyProcessor, Config config) {
        super(workerId, responseBodyProcessor, config);
        this.responseBodyProcessor = responseBodyProcessor;
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        this.requestFactory = new RequestFactory(config().requestType());
    }

    public static void initHttpClient(int threadCount) {
    }

    public static void closeHttpClient() {
    }

    /**
     *  Starts the worker and returns a CompletableFuture, which will be completed, when the worker has finished the
     *  completion target. The CompletableFuture will contain a Result object, which contains the execution stats of the
     *  worker. The execution stats contain the execution time, the http status code, the content length and the hash of
     *  the response body. If the worker failed to execute a query, the execution stats will contain an exception.
     *  If the worker failed to execute a query, because of a set time limit in the worker configuration, the result
     *  of that execution will be discarded.
     *
     * @return the CompletableFuture the contains the results of the worker.
     */
    public CompletableFuture<Result> start() {
        httpClient = new HttpClient(config().connection().endpoint());
        httpClient.connect();

        return CompletableFuture.supplyAsync(() -> {
            ZonedDateTime startTime = ZonedDateTime.now();
            List<ExecutionStats> executionStats = new ArrayList<>();
            if (config().completionTarget() instanceof QueryMixes queryMixes) {
                for (int i = 0; i < queryMixes.number(); i++) {
                    for (int j = 0; j < config().queries().getQueryCount(); j++) {
                        ExecutionStats execution = executeQuery(config().timeout(), false);
                        if (execution == null) throw new RuntimeException("Execution returned null at a place, where it should have never been null.");
                        logExecution(execution);
                        executionStats.add(execution);
                    }
                    LOGGER.info("{}\t:: Completed {} out of {} querymixes.", this, i + 1, queryMixes.number());
                }
            } else if (config().completionTarget() instanceof TimeLimit timeLimit) {
                final var startNanos = System.nanoTime();
                long queryExecutionCount = 0;
                int queryMixExecutionCount = 0;
                int queryMixSize = config().queries().getQueryCount();
                long now;
                while ((now = System.nanoTime()) - startNanos < ((TimeLimit) config.completionTarget()).duration().toNanos()) {
                    final var timeLeft = ((TimeLimit) config.completionTarget()).duration().toNanos() - (now - startNanos);
                    final var reducedTimeout = timeLeft < config.timeout().toNanos();
                    final Duration actualQueryTimeOut = reducedTimeout ? Duration.of(timeLeft, ChronoUnit.NANOS) : config.timeout();
                    ExecutionStats execution = executeQuery(actualQueryTimeOut, reducedTimeout);
                    if (execution != null){ // If timeout is reduced, the execution result might be discarded if it failed and executeQuery returns null.
                        logExecution(execution);
                        executionStats.add(execution);
                    }

                    //
                    if ((++queryExecutionCount) >= queryMixSize) {
                        queryExecutionCount = 0;
                        queryMixExecutionCount++;
                        LOGGER.info("{}\t:: Completed {} querymixes.", this, queryMixExecutionCount);
                    }
                }
                LOGGER.info("{}\t:: Reached time limit of {}.", this, timeLimit.duration());
            }
            ZonedDateTime endTime = ZonedDateTime.now();
            httpClient.close();

            return new Result(this.workerID, executionStats, startTime, endTime);
        }, executor);
    }

    /**
     * Executes the next query given by the query selector from the query handler. If the execution fails and
     * discardOnFailure is true, the execution will be discarded and null will be returned. If the execution fails and
     * discardOnFailure is false, the execution statistic with the failed results will be returned.
     *
     * @param timeout           the timeout for the execution
     * @param discardOnFailure  if true, this method will return null, if the execution fails
     * @return                  the execution statistic of the execution
     */
    private ExecutionStats executeQuery(Duration timeout, boolean discardOnFailure) {
        HttpExecutionResult result = executeHttpRequest(timeout);
        Optional<Integer> statuscode = Optional.empty();
        statuscode = Optional.of(200); // TODO: get status code from response

        if (result.successful() && this.config.parseResults()) { // 2xx
            if (result.actualContentLength.isEmpty() || result.hash.isEmpty() || result.outputStream.isEmpty()) {
                throw new RuntimeException("Response body is null, but execution was successful."); // This should never happen
            }

            // process result
            if (!responseBodyProcessor.add(result.actualContentLength().orElse(-1), result.hash().orElse(-1), result.outputStream().orElse(new BigByteArrayOutputStream()))) {
                this.responseBodybbaos = result.outputStream().orElse(new BigByteArrayOutputStream());
            } else {
                this.responseBodybbaos = new BigByteArrayOutputStream();
            }
        }

        try {
            this.responseBodybbaos.reset();
        } catch (IOException e) {
            this.responseBodybbaos = new BigByteArrayOutputStream();
        }

        // This is not explicitly checking for a timeout, instead it just checks if the execution was successful or not.
        // TODO: This might cause problems if the query actually fails before the timeout and discardOnFailure is true.
        if (!result.successful() && discardOnFailure) {
            LOGGER.debug("{}\t:: Discarded execution, because the time limit has been reached: [queryID={}]", this, result.queryID);
            return null;
        }

        return new ExecutionStats(
                result.queryID(),
                result.requestStart(),
                result.duration(),
                statuscode,
                result.actualContentLength(),
                result.hash,
                result.exception()
        );
    }


    private HttpExecutionResult executeHttpRequest(Duration timeout) {
        final QueryHandler.QueryStreamWrapper queryHandle;
        try {
            queryHandle = config().queries().getNextQueryStream(this.querySelector);
        } catch (IOException e) {
            return new HttpExecutionResult(
                this.querySelector.getCurrentIndex(),
                Optional.empty(),
                Instant.now(),
                Duration.ZERO,
                Optional.empty(),
                OptionalLong.empty(),
                OptionalLong.empty(),
                Optional.of(e)
            );
        }

        final String request;
        try {
            request = requestFactory.buildHttpRequest(
                    queryHandle.queryInputStream(),
                    config().connection(),
                    config().acceptHeader()
            );
        } catch (IOException | URISyntaxException e) {
            return new HttpExecutionResult(
                    queryHandle.index(),
                    Optional.empty(),
                    Instant.now(),
                    Duration.ZERO,
                    Optional.empty(),
                    OptionalLong.empty(),
                    OptionalLong.empty(),
                    Optional.of(e)
            );
        }

        final Instant timeStamp = Instant.now();
        final var requestStart = System.nanoTime();
        httpClient.sendRequest(request);
        final var length = httpClient.awaitResponse();
        final var duration = Duration.ofNanos(System.nanoTime() - requestStart);
        return new HttpExecutionResult(
                queryHandle.index(),
                Optional.empty(),
                timeStamp,
                duration,
                Optional.empty(),
                OptionalLong.of(length),
                OptionalLong.of(0),
                Optional.empty()
        );
    }

    private void logExecution(ExecutionStats execution) {
        switch (execution.endState()) {
            case SUCCESS -> LOGGER.debug("{}\t:: Successfully executed query: [queryID={}].", this, execution.queryID());
            case TIMEOUT -> LOGGER.warn("{}\t:: Timeout during query execution: [queryID={}, duration={}].", this, execution.queryID(), execution.duration()); // TODO: look for a possibility to add the query string for better logging
            case HTTP_ERROR -> LOGGER.warn("{}\t:: HTTP Error occurred during query execution: [queryID={}, httpError={}].", this, execution.queryID(), execution.httpStatusCode().orElse(-1));
            case MISCELLANEOUS_EXCEPTION -> LOGGER.warn("{}\t:: Miscellaneous exception occurred during query execution: [queryID={}, exception={}].", this, execution.queryID(), execution.error().orElse(null));
        }
    }

    @Override
    public String toString() {
        return MessageFormatter.format("[{}-{}]", SPARQLProtocolWorker.class.getSimpleName(), this.workerID).getMessage();
    }
}
