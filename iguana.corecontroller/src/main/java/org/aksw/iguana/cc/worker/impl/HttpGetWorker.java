package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.factory.TypedFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

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


    @Override
    public void executeQuery(String query, String queryID) {
        requestStartTime = Instant.now();
        queryId = queryID;
        resultsSaved = false;
        requestTimedOut = false;

        if (client == null)
            initClient();

        try {
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

            setTimeout(timeOut.intValue());

            response = client.execute(request, getAuthContext(con.getEndpoint()));
            // method to process the result in background
            super.processHttpResponse();

            abortTimeout();

        } catch (ClientProtocolException e) {
            handleException(query, COMMON.QUERY_HTTP_FAILURE, e);
        } catch (IOException e) {
            if (requestTimedOut) {
                LOGGER.warn("Worker[{} : {}]: Reached timout on query (ID {})\n{}",
                        this.workerType, this.workerID, queryId, query);
                super.addResultsOnce(new QueryExecutionStats(queryId, COMMON.QUERY_SOCKET_TIMEOUT, timeOut));
            } else {
                handleException(query, COMMON.QUERY_UNKNOWN_EXCEPTION, e);
            }
        } catch (Exception e) {
            handleException(query, COMMON.QUERY_UNKNOWN_EXCEPTION, e);
        }
        closeResponse();
    }

    private void handleException(String query, Long cause, Exception e) {
        double duration = durationInMilliseconds(requestStartTime, Instant.now());
        super.addResultsOnce(new QueryExecutionStats(queryId, cause, duration));
        LOGGER.warn("Worker[{} : {}]: {} on query (ID {})\n{}",
                this.workerType, this.workerID, e.getMessage(), queryId, query);
        closeClient();
        initClient();
    }

}
