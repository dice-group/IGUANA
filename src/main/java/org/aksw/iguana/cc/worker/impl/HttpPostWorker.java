package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP Post worker.
 * Uses HTTP posts to execute a query.
 * <br/><br/>
 * Sends the query in plain as POST data if parameter type was not set, otherwise uses json as follows:<br/>
 * {PARAMETER: QUERY}
 */
@Shorthand("HttpPostWorker")
public class HttpPostWorker extends HttpGetWorker {

    private String contentType = "text/plain";

    public HttpPostWorker(String taskID, Integer workerID, ConnectionConfig connection, Map<String, Object> queries, @Nullable Integer timeLimit, @Nullable Integer timeOut, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, @Nullable String parameterName, @Nullable String responseType, @Nullable String contentType) {
        super(taskID, workerID, connection, queries, timeLimit, timeOut, fixedLatency, gaussianLatency, parameterName, responseType);

        if (parameterName == null) {
            this.parameter = null;
        }
        if (contentType != null) {
            this.contentType = contentType;
        }
    }

    void buildRequest(String query, String queryID) {
        StringBuilder data = new StringBuilder();
        if (parameter != null) {
            String qEncoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            data.append("{ \"").append(parameter).append("\": \"").append(qEncoded).append("\"}");
        } else {
            data.append(query);
        }
        StringEntity entity = new StringEntity(data.toString(), StandardCharsets.UTF_8);
        request = new HttpPost(con.getUpdateEndpoint());
        ((HttpPost) request).setEntity(entity);
        request.setHeader("Content-Type", contentType);
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(timeOut.intValue())
                .setConnectTimeout(timeOut.intValue())
                .build();

        if (this.responseType != null)
            request.setHeader(HttpHeaders.ACCEPT, this.responseType);

        request.setConfig(requestConfig);
    }

}
