package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.model.QueryResultHashKey;
import org.aksw.iguana.cc.worker.AbstractWorker;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.constants.COMMON;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

/**
 * Abstract HTTP worker
 */
public abstract class HttpWorker extends AbstractWorker {


    protected final ExecutorService resultProcessorService = Executors.newFixedThreadPool(5);
    protected ScheduledThreadPoolExecutor timeoutExecutorPool = new ScheduledThreadPoolExecutor(1);
    protected ConcurrentMap<QueryResultHashKey, Long> processedResults = new ConcurrentHashMap<>();
    protected CloseableHttpClient client;
    protected HttpRequestBase request;
    protected ScheduledFuture<?> abortCurrentRequestFuture;
    protected CloseableHttpResponse response;
    protected boolean resultsSaved = false;
    protected boolean requestTimedOut = false;
    protected String queryId;
    protected Instant requestStartTime;
    protected long tmpExecutedQueries = 0;

    public HttpWorker(String taskID, Integer workerID, Connection connection, Map<String, Object> queries, @Nullable Integer timeLimit, @Nullable Integer timeOut, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency) {
        super(taskID, workerID, connection, queries, timeLimit, timeOut, fixedLatency, gaussianLatency);
        this.timeoutExecutorPool.setRemoveOnCancelPolicy(true);
    }

    public ConcurrentMap<QueryResultHashKey, Long> getProcessedResults() {
        return this.processedResults;
    }

    protected void setTimeout(int timeOut) {
        assert (this.request != null);
        this.abortCurrentRequestFuture = this.timeoutExecutorPool.schedule(
                () -> {
                    synchronized (this) {
                        this.request.abort();
                        this.requestTimedOut = true;
                    }
                },
                timeOut, TimeUnit.MILLISECONDS);
    }

    protected void abortTimeout() {
        if (!this.abortCurrentRequestFuture.isDone()) {
            this.abortCurrentRequestFuture.cancel(false);
        }
    }


    @Override
    public void stopSending() {
        super.stopSending();
        abortTimeout();
        try {
            if (this.request != null && !this.request.isAborted()) {
                this.request.abort();
            }
        } catch (Exception ignored) {
        }
        closeClient();
        shutdownResultProcessor();
    }


    public void shutdownResultProcessor() {
        this.resultProcessorService.shutdown();
        try {
            boolean finished = this.resultProcessorService.awaitTermination(3000, TimeUnit.MILLISECONDS);
            if (!finished) {
                LOGGER.error("Result Processor could be shutdown orderly. Terminating.");
                this.resultProcessorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Could not shut down http result processor: " + e.getLocalizedMessage());
        }

        try {
            boolean finished = this.timeoutExecutorPool.awaitTermination(3000, TimeUnit.MILLISECONDS);
            if (!finished) {
                LOGGER.error("Timeout Executor could be shutdown orderly. Terminating.");
                this.timeoutExecutorPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Could not shut down http timout executor: " + e.getLocalizedMessage());
        }
    }

    synchronized protected void addResultsOnce(QueryExecutionStats queryExecutionStats) {
        if (!this.resultsSaved) {
            addResults(queryExecutionStats);
            this.resultsSaved = true;
        }
    }

    @Override
    public void executeQuery(String query, String queryID) {
        this.queryId = queryID;
        this.resultsSaved = false;
        this.requestTimedOut = false;

        if (this.client == null)
            initClient();

        try {
            buildRequest(query, this.queryId);

            setTimeout(this.timeOut.intValue());

            this.requestStartTime = Instant.now();
            this.response = this.client.execute(this.request, getAuthContext(this.con.getEndpoint()));
            // method to process the result in background
            processHttpResponse();

            abortTimeout();

        } catch (ClientProtocolException e) {
            handleException(query, COMMON.QUERY_HTTP_FAILURE, e);
        } catch (IOException e) {
            if (this.requestTimedOut) {
                LOGGER.warn("Worker[{} : {}]: Reached timeout on query (ID {})\n{}",
                        this.workerType, this.workerID, this.queryId, query);
                addResultsOnce(new QueryExecutionStats(this.queryId, COMMON.QUERY_SOCKET_TIMEOUT, this.timeOut));
            } else {
                handleException(query, COMMON.QUERY_UNKNOWN_EXCEPTION, e);
            }
        } catch (Exception e) {
            handleException(query, COMMON.QUERY_UNKNOWN_EXCEPTION, e);
        } finally {
            abortTimeout();
            closeResponse();
        }
    }

    private void handleException(String query, Long cause, Exception e) {
        double duration = durationInMilliseconds(this.requestStartTime, Instant.now());
        addResultsOnce(new QueryExecutionStats(this.queryId, cause, duration));
        LOGGER.warn("Worker[{} : {}]: {} on query (ID {})\n{}",
                this.workerType, this.workerID, e.getMessage(), this.queryId, query);
        closeClient();
        initClient();
    }

    protected void processHttpResponse() {
        int responseCode = this.response.getStatusLine().getStatusCode();
        boolean responseCodeSuccess = responseCode >= 200 && responseCode < 300;
        boolean responseCodeOK = responseCode == 200;

        if (responseCodeOK) { // response status is OK (200)
            // get content type header
            HttpEntity httpResponse = this.response.getEntity();
            Header contentTypeHeader = new BasicHeader(httpResponse.getContentType().getName(), httpResponse.getContentType().getValue());
            // get content stream
            try (InputStream contentStream = httpResponse.getContent()) {
                // read content stream with resultProcessor, return length, set string in StringBuilder.
                ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
                long length = this.queryHandler.getLanguageProcessor().readResponse(contentStream, responseBody);
                this.tmpExecutedQueries++;
                // check if such a result was already parsed and is cached
                double duration = durationInMilliseconds(this.requestStartTime, Instant.now());
                synchronized (this) {
                    QueryResultHashKey resultCacheKey = new QueryResultHashKey(queryId, length);
                    if (this.processedResults.containsKey(resultCacheKey)) {
                        LOGGER.debug("found result cache key {} ", resultCacheKey);
                        Long preCalculatedResultSize = this.processedResults.get(resultCacheKey);
                        addResultsOnce(new QueryExecutionStats(this.queryId, COMMON.QUERY_SUCCESS, duration, preCalculatedResultSize));
                    } else {
                        // otherwise: parse it. The parsing result is cached for the next time.
                        if (!this.endSignal) {
                            this.resultProcessorService.submit(new HttpResultProcessor(this, this.queryId, duration, contentTypeHeader, responseBody, length));
                            this.resultsSaved = true;
                        }
                    }
                }

            } catch (IOException | TimeoutException e) {
                double duration = durationInMilliseconds(this.requestStartTime, Instant.now());
                addResultsOnce(new QueryExecutionStats(this.queryId, COMMON.QUERY_HTTP_FAILURE, duration));
            }
        } else if (responseCodeSuccess) { // response status is succeeded (2xx) but not OK (200)
            double duration = durationInMilliseconds(this.requestStartTime, Instant.now());
            addResultsOnce(new QueryExecutionStats(this.queryId, COMMON.QUERY_SUCCESS, duration, 0));
        } else { // response status indicates that the query did not succeed (!= 2xx)
            double duration = durationInMilliseconds(this.requestStartTime, Instant.now());
            addResultsOnce(new QueryExecutionStats(this.queryId, COMMON.QUERY_HTTP_FAILURE, duration));
        }
    }

    abstract void buildRequest(String query, String queryID) throws UnsupportedEncodingException;

    protected void initClient() {
        this.client = HttpClients.custom().setConnectionManager(new BasicHttpClientConnectionManager()).build();
    }

    protected void closeClient() {
        closeResponse();
        try {
            if (this.client != null)
                this.client.close();
        } catch (IOException e) {
            LOGGER.error("Could not close http response ", e);
        }
        this.client = null;
    }

    protected void closeResponse() {
        try {
            if (this.response != null)
                this.response.close();
        } catch (IOException e) {
            LOGGER.error("Could not close Client ", e);
        }
        this.response = null;
    }

    /**
     * Http Result Processor, analyzes the http response in the background, if it was cached already, what is the result size,
     * did the response was a success or failure.
     */
    static class HttpResultProcessor implements Runnable {

        private final Logger LOGGER = LoggerFactory.getLogger(getClass());

        private final HttpWorker httpWorker;
        private final String queryId;
        private final double duration;
        private final Header contentTypeHeader;
        private final long contentLength;
        private ByteArrayOutputStream contentStream;

        public HttpResultProcessor(HttpWorker httpWorker, String queryId, double duration, Header contentTypeHeader, ByteArrayOutputStream contentStream, long contentLength) {
            this.httpWorker = httpWorker;
            this.queryId = queryId;
            this.duration = duration;
            this.contentTypeHeader = contentTypeHeader;
            this.contentStream = contentStream;
            this.contentLength = contentLength;
        }

        @Override
        public void run() {
            // Result size is not saved before. Process the http response.

            ConcurrentMap<QueryResultHashKey, Long> processedResults = this.httpWorker.getProcessedResults();
            QueryResultHashKey resultCacheKey = new QueryResultHashKey(this.queryId, this.contentLength);
            try {
                //String content = contentStream.toString(StandardCharsets.UTF_8.name());
                //contentStream = null; // might be hugh, dereference immediately after consumed
                Long resultSize = this.httpWorker.queryHandler.getLanguageProcessor().getResultSize(this.contentTypeHeader, this.contentStream, this.contentLength);
                this.contentStream = null;
                // Save the result size to be re-used
                processedResults.put(resultCacheKey, resultSize);
                LOGGER.debug("added Result Cache Key {}", resultCacheKey);

                this.httpWorker.addResults(new QueryExecutionStats(this.queryId, COMMON.QUERY_SUCCESS, this.duration, resultSize));

            } catch (IOException | ParseException | ParserConfigurationException | SAXException e) {
                LOGGER.error("Query results could not be parsed. ", e);
                this.httpWorker.addResults(new QueryExecutionStats(this.queryId, COMMON.QUERY_UNKNOWN_EXCEPTION, this.duration));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
