package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.lang.impl.SPARQLLanguageProcessor;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.model.QueryResultHashKey;
import org.aksw.iguana.cc.worker.AbstractRandomQueryChooserWorker;
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
import java.util.concurrent.*;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

/**
 * Abstract HTTP worker
 */
public abstract class HttpWorker extends AbstractRandomQueryChooserWorker {


    protected final ExecutorService resultProcessorService = Executors.newFixedThreadPool(5);
    protected ScheduledThreadPoolExecutor timeoutExecutorPool = new ScheduledThreadPoolExecutor(1);
    protected ConcurrentMap<QueryResultHashKey, Long> processedResults = new ConcurrentHashMap<>();
    protected LanguageProcessor resultProcessor = new SPARQLLanguageProcessor();
    protected CloseableHttpClient client;
    protected HttpRequestBase request;
    protected ScheduledFuture<?> abortCurrentRequestFuture;
    protected CloseableHttpResponse response;
    protected boolean resultsSaved = false;
    protected boolean requestTimedOut = false;
    protected String queryId;
    protected Instant requestStartTime;
    protected long tmpExecutedQueries = 0;


    public HttpWorker(String taskID, Connection connection, String queriesFile, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, Integer workerID) {
        super(taskID, connection, queriesFile, timeOut, timeLimit, fixedLatency, gaussianLatency, workerID);
        timeoutExecutorPool.setRemoveOnCancelPolicy(true);
    }

    public ConcurrentMap<QueryResultHashKey, Long> getProcessedResults() {
        return processedResults;
    }

    protected void setTimeout(int timeOut) {
        assert (request != null);
        abortCurrentRequestFuture = timeoutExecutorPool.schedule(
                () -> {
                    synchronized (this) {
                        request.abort();
                        requestTimedOut = true;
                    }
                },
                timeOut, TimeUnit.MILLISECONDS);
    }

    protected void abortTimeout() {
        if (!abortCurrentRequestFuture.isDone())
            abortCurrentRequestFuture.cancel(false);
    }


    @Override
    public void stopSending() {
        super.stopSending();
        abortTimeout();
        try {
            if (request != null && !request.isAborted())
                request.abort();
        } catch (Exception ignored) {
        }
        closeClient();
        this.shutdownResultProcessor();
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

    boolean checkResponseStatus() {
        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode == 200) {
            return true;
        } else {
            double duration = durationInMilliseconds(requestStartTime, Instant.now());
            addResultsOnce(new QueryExecutionStats(queryId, COMMON.QUERY_HTTP_FAILURE, duration));
            return false;
        }
    }

    synchronized protected void addResultsOnce(QueryExecutionStats queryExecutionStats) {
        if (!resultsSaved) {
            this.addResults(queryExecutionStats);
            resultsSaved = true;
        }
    }

    @Override
    public void executeQuery(String query, String queryID) {
        queryId = queryID;
        resultsSaved = false;
        requestTimedOut = false;

        if (client == null)
            initClient();

        try {
            buildRequest(query, queryId);

            setTimeout(timeOut.intValue());
            
            requestStartTime = Instant.now();
            response = client.execute(request, getAuthContext(con.getEndpoint()));
            // method to process the result in background
            processHttpResponse();

            abortTimeout();

        } catch (ClientProtocolException e) {
            handleException(query, COMMON.QUERY_HTTP_FAILURE, e);
        } catch (IOException e) {
            if (requestTimedOut) {
                LOGGER.warn("Worker[{} : {}]: Reached timeout on query (ID {})\n{}",
                        this.workerType, this.workerID, queryId, query);
                addResultsOnce(new QueryExecutionStats(queryId, COMMON.QUERY_SOCKET_TIMEOUT, timeOut));
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
        double duration = durationInMilliseconds(requestStartTime, Instant.now());
        addResultsOnce(new QueryExecutionStats(queryId, cause, duration));
        LOGGER.warn("Worker[{} : {}]: {} on query (ID {})\n{}",
                this.workerType, this.workerID, e.getMessage(), queryId, query);
        closeClient();
        initClient();
    }

    protected void processHttpResponse() {
        // check if query execution took already longer than timeout
        boolean responseCodeOK = checkResponseStatus();
        if (responseCodeOK) { // response status is OK (200)
            // get content type header
            HttpEntity httpResponse = response.getEntity();
            Header contentTypeHeader = new BasicHeader(httpResponse.getContentType().getName(), httpResponse.getContentType().getValue());
            // get content stream
            try (InputStream inputStream = httpResponse.getContent()) {
                // read content stream
                //Stream in resultProcessor, return length, set string in StringBuilder.
                ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
                long length = resultProcessor.readResponse(inputStream, responseBody);
                tmpExecutedQueries++;
                // check if such a result was already parsed and is cached
                double duration = durationInMilliseconds(requestStartTime, Instant.now());
                synchronized (this) {
                    QueryResultHashKey resultCacheKey = new QueryResultHashKey(queryId, length);
                    if (processedResults.containsKey(resultCacheKey)) {
                        LOGGER.debug("found result cache key {} ", resultCacheKey);
                        Long preCalculatedResultSize = processedResults.get(resultCacheKey);
                        addResultsOnce(new QueryExecutionStats(queryId, COMMON.QUERY_SUCCESS, duration, preCalculatedResultSize));
                    } else {
                        // otherwise: parse it. The parsing result is cached for the next time.
                        if (!this.endSignal) {
                            resultProcessorService.submit(new HttpResultProcessor(this, queryId, duration, contentTypeHeader, responseBody, length));
                            resultsSaved = true;
                        }
                    }
                }

            } catch (IOException | TimeoutException e) {
                double duration = durationInMilliseconds(requestStartTime, Instant.now());
                addResultsOnce(new QueryExecutionStats(queryId, COMMON.QUERY_HTTP_FAILURE, duration));
            }
        }
    }

    abstract void buildRequest(String query, String queryID) throws UnsupportedEncodingException;

    protected void initClient() {
        client = HttpClients.custom().setConnectionManager(new BasicHttpClientConnectionManager()).build();
    }

    protected void closeClient() {
        closeResponse();
        try {
            if (client != null)
                client.close();
        } catch (IOException e) {
            LOGGER.error("Could not close http response ", e);
        }
        client = null;
    }

    protected void closeResponse() {
        try {
            if (response != null)
                response.close();
        } catch (IOException e) {
            LOGGER.error("Could not close Client ", e);
        }
        response = null;
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
        private ByteArrayOutputStream contentStream;
        private final long contentLength;

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

            ConcurrentMap<QueryResultHashKey, Long> processedResults = httpWorker.getProcessedResults();
            QueryResultHashKey resultCacheKey = new QueryResultHashKey(queryId, contentLength);
            try {
                //String content = contentStream.toString(StandardCharsets.UTF_8.name());
                //contentStream = null; // might be hugh, dereference immediately after consumed
                Long resultSize = httpWorker.resultProcessor.getResultSize(contentTypeHeader, contentStream, contentLength);
                contentStream = null;
                // Save the result size to be re-used
                processedResults.put(resultCacheKey, resultSize);
                LOGGER.debug("added Result Cache Key {}", resultCacheKey);

                httpWorker.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_SUCCESS, duration, resultSize));

            } catch (IOException | ParseException | ParserConfigurationException | SAXException e) {
                LOGGER.error("Query results could not be parsed. ", e);
                httpWorker.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_UNKNOWN_EXCEPTION, duration));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

