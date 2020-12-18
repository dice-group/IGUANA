package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.factory.TypedFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;


/**
 * HTTP Get Worker.
 * Uses HTTP Get to execute a Query.</br></br>
 * if the parameter type was not set it will use 'query' as the parameter as default, otherwise it will use the provided parameter
 */
@Shorthand("HttpGetWorker")
public class HttpGetWorker extends HttpWorker {

    protected String parameter = "query";

    protected String responseType = null;


    public HttpGetWorker(String taskID, Connection connection, String queriesFile, @Nullable String responseType, @Nullable String parameterName, @Nullable String language, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, @Nullable String workerType, Integer workerID) {
        super(taskID, connection, queriesFile, timeOut, timeLimit, fixedLatency, gaussianLatency, workerType == null ? "HttpGetWorker" : workerType, workerID);
        if (language != null) {
            resultProcessor = new TypedFactory<LanguageProcessor>().create(language, new HashMap<Object, Object>());
        }
        if (parameterName != null) {
            parameter = parameterName;
        }
        if (responseType != null) {
            this.responseType = responseType;
        }
    }

    void buildRequest(String query, String queryID) throws UnsupportedEncodingException {
        String qEncoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String addChar = "?";
        if (con.getEndpoint().contains("?")) {
            addChar = "&";
        }
        String url = con.getEndpoint() + addChar + parameter + "=" + qEncoded;
        request = new HttpGet(url);
        RequestConfig requestConfig =
                RequestConfig.custom()
                        .setSocketTimeout(timeOut.intValue())
                        .setConnectTimeout(timeOut.intValue())
                        .build();

        if (this.responseType != null)
            request.setHeader(HttpHeaders.ACCEPT, this.responseType);

        request.setConfig(requestConfig);
    }
}
