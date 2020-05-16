package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.tp.config.CONSTANTS;
import org.aksw.iguana.tp.model.QueryExecutionStats;
import org.aksw.iguana.tp.utils.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;



/**
 * A Worker using SPARQL 1.1 to create service request.
 *
 * @author f.conrads
 */
public class SPARQLWorker extends HttpWorker {


	private int currentQueryID = 0;

	private Random queryPatternChooser;
	private String responseType;

	/**
	 * @param args
	 */
	public SPARQLWorker(String[] args) {
		super(args, "SPARQL");
		queryPatternChooser = new Random(this.workerID);
	}

	/**
	 *
	 */
	public SPARQLWorker() {
		super("SPARQLWorker");
	}

	@Override
	public void init(String args[]) {
		super.init(args);
		queryPatternChooser = new Random(this.workerID);
	}

	@Override
	public void init(Properties p) {
		super.init(p);
		queryPatternChooser = new Random(this.workerID);

		if(p.containsKey(CONSTANTS.QUERY_RESPONSE_TYPE))
			this.responseType = p.getProperty(CONSTANTS.QUERY_RESPONSE_TYPE);
	}

	@Override
	public void executeQuery(String query, String queryID) {
		Instant start = Instant.now();

		try {
			String qEncoded = URLEncoder.encode(query);
			String addChar = "?";
			if (service.contains("?")) {
				addChar = "&";
			}
			String url = service + addChar + "query=" + qEncoded;
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
		int queryLine = queryPatternChooser.nextInt(queriesInFile);
		queryStr.append(FileUtils.readLineAt(queryLine, currentQueryFile));

		// If there is no more query(Pattern) start from beginning.
		if (this.currentQueryID >= this.queryFileList.length) {
			this.currentQueryID = 0;
		}

	}

	@Override
	public void setQueriesList(File[] queries) {
		super.setQueriesList(queries);
		this.currentQueryID = queryPatternChooser.nextInt(this.queryFileList.length);
	}

}
