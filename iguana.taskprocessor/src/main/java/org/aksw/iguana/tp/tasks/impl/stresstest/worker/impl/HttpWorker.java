package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.tp.model.QueryExecutionStats;
import org.aksw.iguana.tp.model.QueryResultHashKey;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.*;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

public abstract class HttpWorker extends AbstractWorker {

    private ExecutorService resultProcessorService = Executors.newFixedThreadPool(5);
    private ConcurrentMap<QueryResultHashKey, Long> processedResults = new ConcurrentHashMap<>();

    public static final String XML_RESULT_ELEMENT_NAME = "result";
    public static final String XML_RESULT_ROOT_ELEMENT_NAME = "results";
    public static final String QUERY_RESULT_TYPE_JSON = "application/sparql-results+json";
    public static final String QUERY_RESULT_TYPE_XML = "application/sparql-results+xml";

    public HttpWorker(String workerType) {
        super(workerType);
    }

    public HttpWorker(String[] args, String workerType) {
        super(args, workerType);
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
            System.out.println("Could not shut down http result processor: " + e.getLocalizedMessage());
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

    public static String getContentTypeVal(Header header) {
        System.out.println("[DEBUG] HEADER: " + header);
        for (HeaderElement el : header.getElements()) {
            NameValuePair cTypePair = el.getParameterByName("Content-Type");

            if (cTypePair != null && !cTypePair.getValue().isEmpty()) {
                return cTypePair.getValue();
            }
        }
        int index = header.toString().indexOf("Content-Type");
        if (index >= 0) {
            String ret = header.toString().substring(index + "Content-Type".length() + 1);
            if (ret.contains(";")) {
                return ret.substring(0, ret.indexOf(";")).trim();
            }
            return ret.trim();
        }
        return "application/sparql-results+json";
    }

    public static long getJsonResultSize(String res) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(res.trim());
        long size = ((JSONArray) ((JSONObject) json.get("results")).get("bindings")).size();
        return size;
    }

    public static long getXmlResultSize(String res) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        ByteArrayInputStream input = new ByteArrayInputStream(res.getBytes(StandardCharsets.UTF_8));
        Document doc = dBuilder.parse(input);
        NodeList childNodes = doc.getDocumentElement().getElementsByTagName(XML_RESULT_ROOT_ELEMENT_NAME).item(0).getChildNodes();

        long size = 0;
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (XML_RESULT_ELEMENT_NAME.equalsIgnoreCase(childNodes.item(i).getNodeName())) {
                size++;
            }
        }
        return size;

    }
}

class HttpResultProcessor implements Runnable{

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
            System.out.println("found : " + resultCacheKey);
            Long preCalculatedResultSize = processedResults.get(resultCacheKey);
            httpWorker.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_SUCCESS, duration, preCalculatedResultSize));
            return;
        }

        // Result size is not saved before. Process the http response.
        Header[] contentTypeHeaders = response.getHeaders("Content-Type");
        String contentType = HttpWorker.getContentTypeVal(contentTypeHeaders[0]);

        try (InputStream inputStream = httpResponse.getContent();
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line);
            }
            System.out.println("[DEBUG]: byte size: " + result.length());

            long resultSize;
            if (HttpWorker.QUERY_RESULT_TYPE_JSON.equals(contentType)) {
                resultSize = HttpWorker.getJsonResultSize(result.toString());
            } else if (HttpWorker.QUERY_RESULT_TYPE_XML.equals(contentType)) {
                resultSize = HttpWorker.getXmlResultSize(result.toString());
            } else {
                resultSize = StringUtils.countMatches(result.toString(), "\n");
            }

            // Save the result size to be re-used
            processedResults.put(resultCacheKey, resultSize);
            System.out.println("added : " + resultCacheKey);

            response.close();
            client.close();

            httpWorker.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_SUCCESS, duration, resultSize));

        } catch (IOException | ParseException | ParserConfigurationException | SAXException e) {
            System.out.println("Query results could not be parsed: " + e);
            httpWorker.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_UNKNOWN_EXCEPTION, duration));
        }
    }
}