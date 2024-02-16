package org.aksw.iguana.cc.worker.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.query.selector.impl.LinearQuerySelector;
import org.aksw.iguana.cc.utils.http.StreamEntityProducer;
import org.aksw.iguana.cc.worker.ResponseBodyProcessor;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.commons.io.BigByteArrayOutputStream;
import org.apache.hc.client5.http.async.methods.AbstractBinResponseConsumer;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.entity.BasicAsyncEntityProducer;
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.reactor.IOReactorConfig;
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
import java.util.function.Supplier;
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
        private final Map<Integer, AsyncRequestProducer> cache = new HashMap<>();

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

        public AsyncRequestProducer buildHttpRequest(QueryHandler.QueryStreamWrapper queryHandle,
                                                     ConnectionConfig connection,
                                                     String requestHeader) throws URISyntaxException, IOException {
            if (queryHandle.cached() && cache.containsKey(queryHandle.index()))
                return cache.get(queryHandle.index());

            AsyncRequestBuilder asyncRequestBuilder;
            Supplier<InputStream> queryStreamSupplier;
            InputStream queryStream;

            try {
                queryStreamSupplier = queryHandle.queryInputStreamSupplier();
                queryStream = queryStreamSupplier.get();
            } catch (RuntimeException e) {
                throw new IOException(e);
            }

            switch (this.requestType) {
                case GET_QUERY ->
                        asyncRequestBuilder = AsyncRequestBuilder.get(new URIBuilder(connection.endpoint())
                        .addParameter("query", new String(queryStream.readAllBytes(), StandardCharsets.UTF_8))
                        .build()
                );
                case POST_URL_ENC_QUERY ->
                        asyncRequestBuilder = AsyncRequestBuilder.post(connection.endpoint())
                                .setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                                .setEntity(new BasicAsyncEntityProducer(urlEncode("query", new String(queryStream.readAllBytes(), StandardCharsets.UTF_8)), ContentType.APPLICATION_FORM_URLENCODED, !queryHandle.cached()));
                case POST_QUERY ->
                        asyncRequestBuilder = AsyncRequestBuilder.post(connection.endpoint())
                        .setHeader(HttpHeaders.CONTENT_TYPE, "application/sparql-query")
                        .setEntity(new StreamEntityProducer(queryStreamSupplier, !queryHandle.cached()));
                case POST_URL_ENC_UPDATE ->
                        asyncRequestBuilder = AsyncRequestBuilder.post(connection.endpoint())
                        .setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                                .setEntity(new BasicAsyncEntityProducer(urlEncode("update", new String(queryStream.readAllBytes(), StandardCharsets.UTF_8)), ContentType.APPLICATION_FORM_URLENCODED, !queryHandle.cached()));
                case POST_UPDATE ->
                        asyncRequestBuilder = AsyncRequestBuilder.post(connection.endpoint())
                        .setHeader(HttpHeaders.CONTENT_TYPE, "application/sparql-update")
                        .setEntity(new StreamEntityProducer(queryStreamSupplier, !queryHandle.cached()));
                default -> throw new IllegalStateException("Unexpected value: " + this.requestType);
            }

            if (requestHeader != null)
                asyncRequestBuilder.addHeader("Accept", requestHeader);
            if (connection.authentication() != null && connection.authentication().user() != null)
                asyncRequestBuilder.addHeader("Authorization",
                        HttpWorker.basicAuth(connection.authentication().user(),
                                Optional.ofNullable(connection.authentication().password()).orElse("")));

            if (queryHandle.cached())
                cache.put(queryHandle.index(), asyncRequestBuilder.build());

            return asyncRequestBuilder.build();
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
        connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                .setMaxConnTotal(threadCount)
                .setMaxConnPerRoute(threadCount)
                .build();
        final var ioReactorConfig = IOReactorConfig.custom()
                .setTcpNoDelay(true)
                .setIoThreadCount(threadCount)
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
        if (result.response().isPresent())
            statuscode = Optional.of(result.response().get().getCode());

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
            return createFailedResultBeforeRequest(this.querySelector.getCurrentIndex(), e);
        }

        final AsyncRequestProducer request;
        try {
            request = requestFactory.buildHttpRequest(
                    queryHandle,
                    config().connection(),
                    config().acceptHeader()
            );
        } catch (IOException | URISyntaxException e) {
            return createFailedResultBeforeRequest(queryHandle.index(), e);
        }

        final Instant timeStamp = Instant.now();
        final var requestStart = System.nanoTime();
        final var future = httpClient.execute(request, new AbstractBinResponseConsumer<HttpExecutionResult>() {

            private HttpResponse response;
            private final StreamingXXHash64 hasher = hasherFactory.newStreamingHash64(0);
            private long responseSize = 0;
            private long responseEnd = 0;


            @Override
            public void releaseResources() {}

            @Override
            protected int capacityIncrement() {
                return Integer.MAX_VALUE;
            }

            @Override
            protected void data(ByteBuffer src, boolean endOfStream) throws IOException {
                if (endOfStream) {
                    responseEnd = System.nanoTime();
                    return;
                }

                if (config.parseResults()) {
                    if (src.hasArray()) {
                        hasher.update(src.array(), src.position() + src.arrayOffset(), src.remaining());
                        responseBodybbaos.write(src.array(), src.position() + src.arrayOffset(), src.remaining());
                    } else {
                        int readCount;
                        while (src.hasRemaining()) {
                            readCount = Math.min(BUFFER_SIZE, src.remaining());
                            src.get(buffer, 0, readCount);
                            hasher.update(buffer, 0, readCount);
                            responseBodybbaos.write(buffer, 0, readCount);
                        }
                    }
                } else {
                    responseSize += src.remaining();
                }
            }

            @Override
            protected void start(HttpResponse response, ContentType contentType) {
                this.response = response;
            }

            @Override
            protected HttpExecutionResult buildResult() {
                if (responseEnd == 0)
                    responseEnd = System.nanoTime();
                final var duration = Duration.ofNanos(responseEnd - requestStart);

                // http error
                if (response.getCode() / 100 != 2) {
                    return createFailedResultDuringResponse(queryHandle.index(), response, timeStamp, duration, null);
                }

                // check content length
                final var contentLengthHeader = response.getFirstHeader("Content-Length");
                Long contentLength = contentLengthHeader != null ? Long.parseLong(contentLengthHeader.getValue()) : null;
                if (config.parseResults() && contentLength != null && responseBodybbaos.size() != contentLength) {
                    return createFailedResultDuringResponse(queryHandle.index(), response, timeStamp, duration, new HttpException("Content-Length header value doesn't match actual content length."));
                }
                if (!config.parseResults() && contentLength != null && responseSize != contentLength) {
                    return createFailedResultDuringResponse(queryHandle.index(), response, timeStamp, duration, new HttpException("Content-Length header value doesn't match actual content length."));
                }

                // check timeout
                if (duration.compareTo(timeout) > 0) {
                    return createFailedResultDuringResponse(queryHandle.index(), response, timeStamp, duration, new TimeoutException());
                }

                return new HttpExecutionResult(
                        queryHandle.index(),
                        Optional.of(response),
                        timeStamp,
                        Duration.ofNanos(responseEnd - requestStart),
                        Optional.of(responseBodybbaos),
                        OptionalLong.of(config.parseResults() ? responseBodybbaos.size() : responseSize),
                        OptionalLong.of(config.parseResults() ? hasher.getValue() : 0),
                        Optional.empty()
                );
            }
        }, null);

        try {
            return future.get(config.timeout().toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            future.cancel(true);
            return createFailedResultBeforeRequest(queryHandle.index(), e);
        }
    }

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
