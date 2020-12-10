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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

import static org.aksw.iguana.commons.streams.Streams.inputStream2String;
import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

/**
 * Abstract HTTP worker
 */
public abstract class HttpWorker extends AbstractRandomQueryChooserWorker {


    protected final ExecutorService resultProcessorService = Executors.newFixedThreadPool(5);
    protected ScheduledThreadPoolExecutor timeoutExecutorPool;
    protected ConcurrentMap<QueryResultHashKey, Long> processedResults = new ConcurrentHashMap<>();
    protected LanguageProcessor resultProcessor = new SPARQLLanguageProcessor();


    public HttpWorker(String taskID, Connection connection, String queriesFile, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, String workerType, Integer workerID) {
        super(taskID, connection, queriesFile, timeOut, timeLimit, fixedLatency, gaussianLatency, workerType, workerID);
        timeoutExecutorPool = new ScheduledThreadPoolExecutor(3);
        timeoutExecutorPool.setRemoveOnCancelPolicy(true);
    }


    public ConcurrentMap<QueryResultHashKey, Long> getProcessedResults() {
        return processedResults;
    }


    @Override
    public void stopSending(){
        super.stopSending();
        this.shutdownResultProcessor();
    }


    public void shutdownResultProcessor() {
        this.resultProcessorService.shutdown();
        try {
            boolean finished = this.resultProcessorService.awaitTermination(3000, TimeUnit.MILLISECONDS);
            if(!finished){
                LOGGER.error("Result Processor could be shutdown orderly. Terminating.");
                this.resultProcessorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Could not shut down http result processor: " + e.getLocalizedMessage());
        }

        try {
            boolean finished = this.timeoutExecutorPool.awaitTermination(3000, TimeUnit.MILLISECONDS);
            if(!finished){
                LOGGER.error("Timeout Executor could be shutdown orderly. Terminating.");
                this.timeoutExecutorPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Could not shut down http timout executor: " + e.getLocalizedMessage());
        }
    }

    boolean checkInTime(String queryId, double duration, CloseableHttpClient client, CloseableHttpResponse response) {
        if (this.timeOut >= duration) {
            return true;
        } else {
            this.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_SOCKET_TIMEOUT, duration));
            closeHttp(client, response);
            return false;
        }
    }

    boolean checkResponseStatus(String queryId, double duration, CloseableHttpClient client, CloseableHttpResponse response) {
        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode == 200) {
            return true;
        } else {
            this.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_HTTP_FAILURE, duration));
            closeHttp(client, response);
            return false;
        }
    }

    private static class SynchronizedTimeout {
        private boolean read_time_out = false;
        private boolean reading_done = false;

        /**
         * Returns if state change was successful.
         */
        public synchronized boolean ReadTimeoutReached() {
            if (!reading_done) {
                read_time_out = false;
                return true;
            } else {
                return false;
            }
        }

        /**
         * Returns if state change was successful.
         */
        public synchronized boolean readingDone() {
            if (!read_time_out) {
                reading_done = false;
                return true;
            } else {
                return false;
            }
        }
    }

    protected void processHttpResponse(String queryId, Instant startTime, CloseableHttpClient client, CloseableHttpResponse response) {
        // check if query execution took already longer than timeout
        double duration = durationInMilliseconds(startTime, Instant.now());
        boolean inTime = checkInTime(queryId, duration, client, response);
        if (inTime) { // we are in time
            // check the response status
            boolean responseCodeOK = checkResponseStatus(queryId, duration, client, response);
            if (responseCodeOK) { // response status is OK (200)
                // get content type header
                HttpEntity httpResponse = response.getEntity();
                Header contentTypeHeader = new BasicHeader(httpResponse.getContentType().getName(), httpResponse.getContentType().getValue());
                // get content stream
                try (InputStream inputStream = httpResponse.getContent()) {
                    try {
                        // read content stream
                        SynchronizedTimeout syncingTimeout = new SynchronizedTimeout();

                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                if (syncingTimeout.ReadTimeoutReached())
                                    closeHttp(client, response);
                            }
                        };
                        new Timer(true).schedule(task, (long) (timeOut - duration));

                        String response_body_string = inputStream2String(inputStream, startTime, timeOut); // may throw TimeoutException
                        if (!syncingTimeout.readingDone())
                            throw new TimeoutException("reading the answer timed out");

                        duration = durationInMilliseconds(startTime, Instant.now());

                        boolean stillInTime = checkInTime(queryId, duration, client, response);
                        if (stillInTime) {  // check if we are still in time
                            // check if such a result was already parsed and is cached
                            QueryResultHashKey resultCacheKey = new QueryResultHashKey(queryId, response_body_string.length());
                            if (processedResults.containsKey(resultCacheKey)) {
                                LOGGER.debug("found result cache key {} ", resultCacheKey);
                                Long preCalculatedResultSize = processedResults.get(resultCacheKey);
                                this.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_SUCCESS, duration, preCalculatedResultSize));
                            } else {
                                // otherwise: parse it. The parsing result is cached for the next time.
                                if (!this.endSignal) {
                                    resultProcessorService.submit(new HttpResultProcessor(this, queryId, duration, contentTypeHeader, response_body_string));
                                }

                            }
                        }
                    } catch (TimeoutException e) {
                        this.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_SOCKET_TIMEOUT, duration));
                    }

                } catch (IOException e) {
                    this.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_HTTP_FAILURE, duration));
                }
            }
        }
        closeHttp(client, response);
    }

    protected static void closeHttp(CloseableHttpClient client, CloseableHttpResponse response) {
        try {
            if (client != null)
                client.close();
        } catch (IOException e) {
            LOGGER.error("Could not close http response ", e);
        }
        try {
            if (response != null)
                response.close();
        } catch (IOException e) {
            LOGGER.error("Could not close Client ", e);
        }
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
        Header contentTypeHeader;
        String content;

        public HttpResultProcessor(HttpWorker httpWorker, String queryId, double duration, Header contentTypeHeader, String content) {
            this.httpWorker = httpWorker;
            this.queryId = queryId;
            this.duration = duration;
            this.contentTypeHeader = contentTypeHeader;
            this.content = content;
        }

        @Override
        public void run() {

            // Result size is not saved before. Process the http response.

            ConcurrentMap<QueryResultHashKey, Long> processedResults = httpWorker.getProcessedResults();
            QueryResultHashKey resultCacheKey = new QueryResultHashKey(queryId, content.length());
            try {
                Long resultSize = httpWorker.resultProcessor.getResultSize(contentTypeHeader, content);
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

