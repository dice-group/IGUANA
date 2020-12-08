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
import org.apache.http.util.EntityUtils;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.time.Instant;
import java.util.concurrent.*;

import static org.aksw.iguana.commons.streams.Streams.inputStream2String;
import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

/**
 * Abstract HTTP worker
 */
public abstract class HttpWorker extends AbstractRandomQueryChooserWorker {


    private final ExecutorService resultProcessorService = Executors.newFixedThreadPool(5);
    protected ConcurrentMap<QueryResultHashKey, Long> processedResults = new ConcurrentHashMap<>();
    protected LanguageProcessor resultProcessor = new SPARQLLanguageProcessor();


    public HttpWorker(String taskID, Connection connection, String queriesFile, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, String workerType, Integer workerID) {
        super(taskID, connection, queriesFile, timeOut, timeLimit, fixedLatency, gaussianLatency, workerType, workerID);
    }


    public ConcurrentMap<QueryResultHashKey, Long> getProcessedResults() {
        return processedResults;
    }

    public void shutdownResultProcessor() {
        this.resultProcessorService.shutdown();
        try {
            this.resultProcessorService.awaitTermination(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("Could not shut down http result processor: " + e.getLocalizedMessage());
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

    protected void processHttpResponse(String queryId, Instant startTime, CloseableHttpClient client, CloseableHttpResponse response) {
        // check if query execution took longer than timeout
        double duration = durationInMilliseconds(startTime, Instant.now());
        boolean inTime = checkInTime(queryId, duration, client, response);
        if (inTime) {
            boolean responseCodeOK = checkResponseStatus(queryId, duration, client, response);
            if (responseCodeOK) {
                HttpEntity httpResponse = response.getEntity();
                ConcurrentMap<QueryResultHashKey, Long> processedResults = this.getProcessedResults();
                QueryResultHashKey resultCacheKey = new QueryResultHashKey(queryId, httpResponse.getContentLength());

                if (processedResults.containsKey(resultCacheKey)) {
                    LOGGER.debug("found result cache key {} ", resultCacheKey);
                    try {
                        EntityUtils.consume(httpResponse);

                        duration = durationInMilliseconds(startTime, Instant.now()); // update duration after consuming the whole Entity
                        boolean stillInTime = checkInTime(queryId, duration, client, response);
                        if (stillInTime) {
                            Long preCalculatedResultSize = processedResults.get(resultCacheKey);
                            this.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_SUCCESS, duration, preCalculatedResultSize));
                        }
                    } catch (IOException e) {
                        this.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_HTTP_FAILURE, duration));
                    }
                } else {
                    if (!this.endSignal) {
                        try (InputStream inputStream = httpResponse.getContent()) {
                            Header contentTypeHeader = new BasicHeader(httpResponse.getContentType().getName(), httpResponse.getContentType().getValue());
                            String response_body_string = inputStream2String(inputStream);
                            duration = durationInMilliseconds(startTime, Instant.now());
                            boolean stillInTime = checkInTime(queryId, duration, client, response);
                            if (stillInTime) {
                                resultProcessorService.submit(new HttpResultProcessor(this, queryId, duration, contentTypeHeader, response_body_string));
                                return;
                            }
                        } catch (IOException e) {
                            this.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_HTTP_FAILURE, duration));
                        }
                    } else {
                        this.shutdownResultProcessor();
                    }
                }
            }
        }
        closeHttp(client, response);
    }

    protected void closeHttp(CloseableHttpClient client, CloseableHttpResponse response) {
        try {
            client.close();
        } catch (IOException e) {
            LOGGER.error("Could not close http response ", e);
        }
        try {
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

