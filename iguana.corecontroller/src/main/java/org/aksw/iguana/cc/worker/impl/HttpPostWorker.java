package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.net.URLEncoder;
import java.time.Instant;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

/**
 * HTTP Post worker.
 * Uses HTTP posts to execute a query.
 * </br></br>
 * Sends the query in plain as POST data if parameter type was not set, otherwise uses json as follows:</br>
 * {PARAMETER: QUERY}
 */
@Shorthand("HttpPostWorker")
public class  HttpPostWorker extends HttpGetWorker {

    private String contentType = "text/plain";
    protected long tmpExecutedQueries=0;


    public HttpPostWorker(String taskID, Connection connection, String queriesFile, @Nullable String contentType, @Nullable String responseType, @Nullable String parameterName, @Nullable String language, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, @Nullable String workerType, Integer workerID) {
        super(taskID, connection, queriesFile, responseType, parameterName, language, timeOut, timeLimit, fixedLatency, gaussianLatency, workerType, workerID);
        if(parameterName==null){
            parameter=null;
        }
        if(contentType!=null){
            this.contentType = contentType;
        }
    }

    @Override
    public void executeQuery(String query, String queryID) {
        Instant start = Instant.now();

        try {
            HttpPost request = new HttpPost(con.getUpdateEndpoint());

            StringBuilder data = new StringBuilder();
            if(parameter!=null){
                String qEncoded = URLEncoder.encode(query, "UTF-8");
                data.append("{ \""+parameter+"\": \"").append(qEncoded).append("\"}");
            }else{
                data.append(query);
            }
            StringEntity entity = new StringEntity(data.toString());
            request.setEntity(entity);
            request.setHeader("Content-Type", contentType);
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeOut.intValue())
                    .setConnectTimeout(timeOut.intValue()).build();

            if(this.responseType != null)
                request.setHeader(HttpHeaders.ACCEPT, this.responseType);

            request.setConfig(requestConfig);

            CloseableHttpClient client = HttpClients.createDefault();
            CloseableHttpResponse response = client.execute(request, getAuthContext(con.getUpdateEndpoint()));

            // method to process the result in background
            super.processHttpResponse(queryID, start, client, response);

            tmpExecutedQueries++;
        } catch (Exception e) {
            LOGGER.warn("Worker[{{}} : {{}}]: Could not execute the following query\n{{}}\n due to", this.workerType,
                    this.workerID, query, e);
            super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now())));
        }
    }
}
