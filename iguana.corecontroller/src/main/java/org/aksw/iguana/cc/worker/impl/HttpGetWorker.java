package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.factory.TypedFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.net.URLEncoder;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

/**
 * HTTP Get Worker.
 * Uses HTTP Get to execute a Query.</br></br>
 * if the parameter type was not set it will use 'query' as the parameter as default, otherwise it will use the provided parameter
 *
 */
@Shorthand("HttpGetWorker")
public class HttpGetWorker extends HttpWorker {

    protected String parameter="query";

    protected String responseType=null;


    public HttpGetWorker(String taskID, Connection connection, String queriesFile, @Nullable String responseType, @Nullable String parameterName, @Nullable String language, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, @Nullable String workerType, Integer workerID) {
        super(taskID, connection, queriesFile, timeOut, timeLimit, fixedLatency, gaussianLatency, workerType==null?"HttpGetWorker":workerType, workerID);
        if(language!=null){
            resultProcessor = new TypedFactory<LanguageProcessor>().create(language, new HashMap<Object, Object>());
        }
        if(parameterName!=null){
            parameter = parameterName;
        }
        if(responseType!=null){
            this.responseType=responseType;
        }

    }


    @Override
    public void executeQuery(String query, String queryID) {
        Instant start = Instant.now();

        try {
            String qEncoded = URLEncoder.encode(query, "UTF-8");
            String addChar = "?";
            if (con.getEndpoint().contains("?")) {
                addChar = "&";
            }
            String url = con.getEndpoint() + addChar + parameter+"=" + qEncoded;
            HttpGet request = new HttpGet(url);
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeOut.intValue())
                    .setConnectTimeout(timeOut.intValue()).build();

            if(this.responseType != null)
                request.setHeader(HttpHeaders.ACCEPT, this.responseType);

            request.setConfig(requestConfig);

            CloseableHttpClient client = HttpClients.createDefault();
            setTimeout(request, timeOut.intValue());

            CloseableHttpResponse response = client.execute(request, getAuthContext(con.getEndpoint()));

            // method to process the result in background
            super.processHttpResponse(queryID, start, client, response);

        } catch (Exception e) {
            LOGGER.warn("Worker[{{}} : {{}}]: Could not execute the following query\n{{}}\n due to", this.workerType,
                    this.workerID, query, e);
            super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now())));
        }
    }

    private void setTimeout(HttpGet http, int timeOut){
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
        ex.schedule(() -> http.abort(), timeOut, TimeUnit.MILLISECONDS);
    }

}
