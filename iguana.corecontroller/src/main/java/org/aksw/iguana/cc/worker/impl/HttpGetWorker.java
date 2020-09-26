package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.utils.FileUtils;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.factory.TypedFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.HashMap;
import java.util.Random;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

/**
 * HTTP Get Worker.
 * Uses HTTP Get to execute a Query.</br></br>
 * if the parameter type was not set it will use 'query' as the parameter as default, otherwise it will use the provided parameter
 *
 */
@Shorthand("HttpGetWorker")
public class HttpGetWorker extends HttpWorker {

    protected int currentQueryID = 0;
    protected String parameter="query";

    protected Random queryChooser;
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
        queryChooser = new Random(this.workerID);

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
            CloseableHttpResponse response = client.execute(request);

            // method to process the result in background
            super.processHttpResponse(queryID, start, client, response);

        } catch (Exception e) {
            LOGGER.warn("Worker[{{}} : {{}}]: Could not execute the following query\n{{}}\n due to", this.workerType,
                    this.workerID, query, e);
            super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now())));
        }
    }

    @Override
    public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
        // get next Query File and next random Query out of it.
        File currentQueryFile = this.queryFileList[this.currentQueryID++];
        queryID.append(currentQueryFile.getName());

        int queriesInFile = FileUtils.countLines(currentQueryFile);
        int queryLine = queryChooser.nextInt(queriesInFile);
        queryStr.append(FileUtils.readLineAt(queryLine, currentQueryFile));

        // If there is no more query(Pattern) start from beginning.
        if (this.currentQueryID >= this.queryFileList.length) {
            this.currentQueryID = 0;
        }
    }



    @Override
    public void setQueriesList(File[] queries) {
        super.setQueriesList(queries);
        this.currentQueryID = queryChooser.nextInt(this.queryFileList.length);
    }
}
