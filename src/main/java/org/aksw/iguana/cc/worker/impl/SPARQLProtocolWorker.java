package org.aksw.iguana.cc.worker.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.query.selector.impl.LinearQuerySelector;
import org.aksw.iguana.cc.utils.http.RequestFactory;
import org.aksw.iguana.cc.worker.ResponseBodyProcessor;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.commons.io.BigByteArrayOutputStream;
import org.apache.hc.client5.http.async.methods.AbstractBinResponseConsumer;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

public class SPARQLProtocolWorker extends HttpWorker {

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
            Optional<HttpResponse> response,
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
            if (response.isPresent() && exception.isEmpty())
                return (response.get().getCode() / 100) == 2;
            return false;
        }
    }


    private static CloseableHttpAsyncClient httpClient;
    private static AsyncClientConnectionManager connectionManager;
    private final ThreadPoolExecutor executor;

    private final XXHashFactory hasherFactory = XXHashFactory.fastestJavaInstance();
    private final RequestFactory requestFactory;

    private final ResponseBodyProcessor responseBodyProcessor;

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

    /**
     * Initializes the http client with the given thread count.
     * All workers will use the same http client instance.
     *
     * @param threadCount the number of threads to be used by the http client
     */
    public static void initHttpClient(int threadCount) {
        connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                .setMaxConnTotal(threadCount * 1000)
                .setMaxConnPerRoute(threadCount * 1000)
                .build();
        final var ioReactorConfig = IOReactorConfig.custom()
                .setTcpNoDelay(true)
                .setSoKeepAlive(true)
                .build();
        httpClient = HttpAsyncClients.custom()
                .setConnectionManager(connectionManager)
                .setIOReactorConfig(ioReactorConfig)
                .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setContentCompressionEnabled(false)
                        .setHardCancellationEnabled(true)
                        .build())
                .build();
        httpClient.start();
    }

    /**
     * Closes the http client and the connection manager.
     */
    public static void closeHttpClient() {
        try {
            httpClient.close();
            connectionManager.close();
        } catch (IOException e) {
            LOGGER.error("Failed to close http client.", e);
        }
    }

    /**
     * Builds every request once, so that the requests can be loaded into the cache, if the queries themselves are
     * cached.
     * This is done to avoid the overhead of building (url-encoding) the requests during the benchmark.
     */
    private void preloadRequests() {
        final var selector = new LinearQuerySelector(config().queries().getQueryCount());
        for (int i = 0; i < config().queries().getQueryCount(); i++) {
            try {
                // build request and discard it
                requestFactory.buildHttpRequest(config().queries().getNextQueryStream(selector), config().connection(), config().acceptHeader());
            } catch (IOException | URISyntaxException e) {
                LOGGER.error("Failed to preload request.", e);
            }
        }
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
        preloadRequests();
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

                    if ((++queryExecutionCount) >= queryMixSize) {
                        queryExecutionCount = 0;
                        queryMixExecutionCount++;
                        LOGGER.info("{}\t:: Completed {} querymixes.", this, queryMixExecutionCount);
                    }
                }
                LOGGER.info("{}\t:: Reached time limit of {}.", this, timeLimit.duration());
            }
            ZonedDateTime endTime = ZonedDateTime.now();
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
        // execute the request
        HttpExecutionResult result = executeHttpRequest(timeout);

        // process result
        Optional<Integer> statuscode = Optional.empty();
        if (result.response().isPresent())
            statuscode = Optional.of(result.response().get().getCode());

        if (result.successful() && this.config.parseResults()) { // 2xx
            if (result.actualContentLength.isEmpty() || result.hash.isEmpty() || result.outputStream.isEmpty()) {
                throw new RuntimeException("Response body is null, but execution was successful."); // This should never happen, just here for fixing the warning.
            }

            // process result
            responseBodyProcessor.add(result.actualContentLength().orElse(-1), result.hash().orElse(-1), result.outputStream().orElse(new BigByteArrayOutputStream()));
        }

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

    /**
     * Executes the next query given by the query selector from the query handler.
     * It uses the http client to execute the request and returns the result of the execution.
     *
     * @param timeout the timeout for the execution
     * @return        the execution result of the execution
     */
    private HttpExecutionResult executeHttpRequest(Duration timeout) {
        // get the next query and request
        final AsyncRequestProducer request;
        final int queryIndex;
        if (config().queries().getConfig().caching()) {
            queryIndex = querySelector.getNextIndex();
            request = requestFactory.getCachedRequest(queryIndex);
        } else {
            final QueryHandler.QueryStreamWrapper queryHandle;
            try {
                queryHandle = config().queries().getNextQueryStream(this.querySelector);
            } catch (IOException e) {
                return createFailedResultBeforeRequest(this.querySelector.getCurrentIndex(), e);
            }

            try {
                request = requestFactory.buildHttpRequest(
                        queryHandle,
                        config().connection(),
                        config().acceptHeader()
                );
            } catch (IOException | URISyntaxException e) {
                return createFailedResultBeforeRequest(queryHandle.index(), e);
            }

            // set queryIndex to the index of the queryHandle, so that the result can be associated with the query
            queryIndex = queryHandle.index();
        }

        // execute the request
        final Instant timeStamp = Instant.now();
        final var requestStart = System.nanoTime();
        final var future = httpClient.execute(request, new AbstractBinResponseConsumer<HttpExecutionResult>() {

            private HttpResponse response;
            private final StreamingXXHash64 hasher = hasherFactory.newStreamingHash64(0);
            private long responseSize = 0; // will be used if parseResults is false
            private long responseEnd = 0;  // time in nanos
            private BigByteArrayOutputStream responseBodybbaos = null;

            @Override
            public void releaseResources() {} // nothing to release

            @Override
            protected int capacityIncrement() {
                return Integer.MAX_VALUE - 8; // get as much data in as possible
            }

            /**
             * Triggered to pass incoming data packet to the data consumer.
             *
             * @param src the data packet.
             * @param endOfStream flag indicating whether this data packet is the last in the data stream.
             */
            @Override
            protected void data(ByteBuffer src, boolean endOfStream) throws IOException {
                if (endOfStream)
                    responseEnd = System.nanoTime();

                if (responseBodybbaos == null)
                    responseBodybbaos = new BigByteArrayOutputStream();

                responseSize += src.remaining();
                if (config.parseResults()) {
                    // if the buffer uses an array, use the array directly
                    if (src.hasArray()) {
                        hasher.update(src.array(), src.position() + src.arrayOffset(), src.remaining());
                        responseBodybbaos.write(src.array(), src.position() + src.arrayOffset(), src.remaining());
                    } else { // otherwise, copy the buffer to an array
                        int readCount;
                        while (src.hasRemaining()) {
                            readCount = Math.min(BUFFER_SIZE, src.remaining());
                            src.get(buffer, 0, readCount);
                            hasher.update(buffer, 0, readCount);
                            responseBodybbaos.write(buffer, 0, readCount);
                        }
                    }
                }
            }

            /**
             * Triggered to signal the beginning of response processing.
             *
             * @param response the response message head
             * @param contentType the content type of the response body,
             *                    or {@code null} if the response does not enclose a response entity.
             */
            @Override
            protected void start(HttpResponse response, ContentType contentType) {
                this.response = response;
                final var contentLengthHeader = response.getFirstHeader("Content-Length");
                Long contentLength = contentLengthHeader != null ? Long.parseLong(contentLengthHeader.getValue()) : null;
                // if the content length is known, create a BigByteArrayOutputStream with the known length
                if (contentLength != null && responseBodybbaos == null && config.parseResults()) {
                    responseBodybbaos = new BigByteArrayOutputStream(contentLength);
                }
            }

            /**
             * Triggered to generate an object that represents a result of response message processing.
             *
             * @return the result of response message processing
             */
            @Override
            protected HttpExecutionResult buildResult() {
                // if the responseEnd hasn't been set yet, set it to the current time
                if (responseEnd == 0)
                    responseEnd = System.nanoTime();

                // duration of the execution
                final var duration = Duration.ofNanos(responseEnd - requestStart);

                // check for http error
                if (response.getCode() / 100 != 2) {
                    return createFailedResultDuringResponse(queryIndex, response, timeStamp, duration, null);
                }

                // check content length
                final var contentLengthHeader = response.getFirstHeader("Content-Length");
                Long contentLength = contentLengthHeader != null ? Long.parseLong(contentLengthHeader.getValue()) : null;
                if (contentLength != null) {
                    if ((!config.parseResults() && responseSize != contentLength) // if parseResults is false, the responseSize will be used
                            || (config.parseResults() && responseBodybbaos.size() != contentLength)) { // if parseResults is true, the size of the bbaos will be used
                        if (responseSize != responseBodybbaos.size())
                            LOGGER.error("Error during copying the response data. (expected written data size = {}, actual written data size = {}, Content-Length-Header = {})", responseSize, responseBodybbaos.size(), contentLengthHeader.getValue());
                        final var exception = new HttpException(String.format("Content-Length header value doesn't match actual content length. (Content-Length-Header = %s, written data size = %s)", contentLength, config.parseResults() ? responseBodybbaos.size() : responseSize));
                        return createFailedResultDuringResponse(queryIndex, response, timeStamp, duration, exception);
                    }
                }

                // check timeout
                if (duration.compareTo(timeout) > 0) {
                    return createFailedResultDuringResponse(queryIndex, response, timeStamp, duration, new TimeoutException());
                }

                // return successful result
                return new HttpExecutionResult(
                        queryIndex,
                        Optional.of(response),
                        timeStamp,
                        Duration.ofNanos(responseEnd - requestStart),
                        Optional.of(responseBodybbaos),
                        OptionalLong.of(config.parseResults() ? responseBodybbaos.size() : responseSize),
                        OptionalLong.of(config.parseResults() ? hasher.getValue() : 0),
                        Optional.empty()
                );
            }
        }, null); // the callback is used to handle the end state of the request, but it's not needed here

        try {
            // Wait for the request to finish, but don't wait longer than the timeout.
            // The timeout from the configuration is used instead of the timeout from the parameter.
            // The timeout from the parameter might be reduced if the end of the time limit is near
            // and it might be so small that it causes issues.
            return future.get(config.timeout().toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException | ExecutionException e) {
            // This will close the connection and cancel the request if it's still running.
            future.cancel(true);
            return createFailedResultBeforeRequest(queryIndex, e);
        } catch (TimeoutException e) {
            if (future.isDone()) {
                LOGGER.warn("Request was already done after timeout.");
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException ex) {
                    return createFailedResultBeforeRequest(queryIndex, ex);
                }
            } else {
                future.cancel(true);
                return createFailedResultBeforeRequest(queryIndex, e);
            }
        }
    }

    /**
     * Creates a failed result for a query execution that failed before the request.
     *
     * @param queryIndex the index of the query
     * @param e          the exception that caused the error
     * @return           the failed result
     */
    private static HttpExecutionResult createFailedResultBeforeRequest(int queryIndex, Exception e) {
        return new HttpExecutionResult(
                queryIndex,
                Optional.empty(),
                Instant.now(),
                Duration.ZERO,
                Optional.empty(),
                OptionalLong.empty(),
                OptionalLong.empty(),
                Optional.ofNullable(e)
        );
    }

    /**
     * Creates a failed result for a query execution that failed during the response.
     *
     * @param queryIndex the index of the query
     * @param response   the response of the query
     * @param timestamp  the start time of the query
     * @param duration   the duration of the query until error
     * @param e          the exception that caused the error, can be null
     * @return           the failed result
     */
    private static HttpExecutionResult createFailedResultDuringResponse(
            int queryIndex,
            HttpResponse response,
            Instant timestamp,
            Duration duration,
            Exception e) {
        return new HttpExecutionResult(
                queryIndex,
                Optional.ofNullable(response),
                timestamp,
                duration,
                Optional.empty(),
                OptionalLong.empty(),
                OptionalLong.empty(),
                Optional.ofNullable(e)
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
