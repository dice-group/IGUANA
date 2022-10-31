package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;


/**
 * HTTP Get Worker.
 * Uses HTTP Get to execute a Query.<br/><br/>
 * if the parameter type was not set it will use 'query' as the parameter as default, otherwise it will use the provided parameter
 */
@Shorthand("HttpGetWorker")
public class HttpGetWorker extends HttpWorker {

    protected String parameter = "query";

    protected String responseType = null;

    public HttpGetWorker(String taskID, Integer workerID, Connection connection, Map<Object, Object> queries, @Nullable Integer timeLimit, @Nullable Integer timeOut, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, @Nullable String parameterName, @Nullable String responseType) {
        super(taskID, workerID, connection, queries, timeLimit, timeOut, fixedLatency, gaussianLatency);

        if (parameterName != null) {
            this.parameter = parameterName;
        }
        if (responseType != null) {
            this.responseType = responseType;
        }
    }

    void buildRequest(String query, String queryID) {
        String qEncoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String addChar = "?";
        if (this.con.getEndpoint().contains("?")) {
            addChar = "&";
        }
        String url = this.con.getEndpoint() + addChar + this.parameter + "=" + qEncoded;
        this.request = new HttpGet(url);
        RequestConfig requestConfig =
                RequestConfig.custom()
                        .setSocketTimeout(this.timeOut.intValue())
                        .setConnectTimeout(this.timeOut.intValue())
                        .build();

        if (this.responseType != null)
            this.request.setHeader(HttpHeaders.ACCEPT, this.responseType);

        this.request.setConfig(requestConfig);
    }
}
