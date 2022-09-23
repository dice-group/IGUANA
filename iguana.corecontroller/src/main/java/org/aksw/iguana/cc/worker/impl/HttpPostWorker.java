package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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


    public HttpPostWorker(String taskID, Connection connection, String queriesFile, @Nullable String contentType, @Nullable String responseType, @Nullable String parameterName, @Nullable String language, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, @Nullable String workerType, Integer workerID) {
        super(taskID, connection, queriesFile, responseType, parameterName, language, timeOut, timeLimit, fixedLatency, gaussianLatency, workerType == null ? "HttpPostWorker" : workerType, workerID);
        if (parameterName == null) {
            parameter = null;
        }
        if (contentType != null) {
            this.contentType = contentType;
        }
    }

    void buildRequest(String query, String queryID) throws UnsupportedEncodingException {
        StringBuilder data = new StringBuilder();
        if (parameter != null) {
            String qEncoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            data.append("{ \"" + parameter + "\": \"").append(qEncoded).append("\"}");
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
