package org.aksw.iguana.cc.tasks.impl.stresstest.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.lang.impl.SPARQLLanguageProcessor;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.model.QueryResultHashKey;
import org.aksw.iguana.cc.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.constants.COMMON;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.*;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

public abstract class HttpWorker extends AbstractWorker {


    private ExecutorService resultProcessorService = Executors.newFixedThreadPool(5);
    private ConcurrentMap<QueryResultHashKey, Long> processedResults = new ConcurrentHashMap<>();
    protected LanguageProcessor resultProcessor = new SPARQLLanguageProcessor();

    public static final String XML_RESULT_ELEMENT_NAME = "result";
    public static final String XML_RESULT_ROOT_ELEMENT_NAME = "results";
    public static final String QUERY_RESULT_TYPE_JSON = "application/sparql-results+json";
    public static final String QUERY_RESULT_TYPE_XML = "application/sparql-results+xml";

    public HttpWorker(String taskID, Connection connection, String queriesFile, @Nullable  Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, String workerType, Integer workerID) {
        super(taskID, connection, queriesFile, timeOut, timeLimit, fixedLatency, gaussianLatency, workerType, workerID);
    }


    public ConcurrentMap<QueryResultHashKey, Long> getProcessedResults() {
        return processedResults;
    }

    public void shutdownResultProcessor()
    {
        this.resultProcessorService.shutdown();
        try {
            this.resultProcessorService.awaitTermination(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("Could not shut down http result processor: " + e.getLocalizedMessage());
        }
    }

    protected void processHttpResponse(String queryId, Instant startTime, CloseableHttpClient client, CloseableHttpResponse response) {
        double duration = durationInMilliseconds(startTime, Instant.now());
        if(!this.endSignal) {
            resultProcessorService.submit(new HttpResultProcessor(this, this.timeOut, queryId, duration, client, response));
        } else {
            this.shutdownResultProcessor();
        }
    }

}

class HttpResultProcessor implements Runnable{

    private Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final double timeOut;
    private final HttpWorker httpWorker;
    private String queryId;
    private double duration;
    private CloseableHttpClient client;
    private CloseableHttpResponse response;

    public HttpResultProcessor(HttpWorker httpWorker, Double timeOut, String queryId, double duration, CloseableHttpClient client, CloseableHttpResponse response)
    {
        this.httpWorker = httpWorker;
        this.timeOut = timeOut;
        this.queryId = queryId;
        this.duration = duration;
        this.client = client;
        this.response = response;
    }

    @Override
    public void run() {
        // check if query execution took longer than timeout
        if (this.timeOut < duration) {
            httpWorker.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_SOCKET_TIMEOUT, duration));
            return;
        }

        // check if there was a problem with http response
        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode != 200) {
            httpWorker.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_HTTP_FAILURE, duration));
            return;
        }

        // Check if the result of this query is already saved, if yes then use the saved result size instead of
        // processing the http response again
        HttpEntity httpResponse = response.getEntity();
        ConcurrentMap<QueryResultHashKey, Long> processedResults = httpWorker.getProcessedResults();
        QueryResultHashKey resultCacheKey = new QueryResultHashKey(queryId, httpResponse.getContentLength());
        if(processedResults.containsKey(resultCacheKey))
        {
            LOGGER.debug("found result cache key {} ", resultCacheKey);
            Long preCalculatedResultSize = processedResults.get(resultCacheKey);
            httpWorker.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_SUCCESS, duration, preCalculatedResultSize));
            return;
        }

        // Result size is not saved before. Process the http response.


        try {

            Long resultSize = httpWorker.resultProcessor.getResultSize(response);
            // Save the result size to be re-used
            processedResults.put(resultCacheKey, resultSize);
            LOGGER.debug("added Result Cache Key {}", resultCacheKey);

            response.close();
            client.close();

            httpWorker.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_SUCCESS, duration, resultSize));

        } catch (IOException | ParseException | ParserConfigurationException | SAXException e) {
            LOGGER.error("Query results could not be parsed. ", e);
            httpWorker.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_UNKNOWN_EXCEPTION, duration));
        }
    }
}