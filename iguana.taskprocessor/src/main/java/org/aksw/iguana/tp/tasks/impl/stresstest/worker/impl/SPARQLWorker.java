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


//import org.json.*;

/**
 * A Worker using SPARQL 1.1 to create service request.
 *
 * @author f.conrads
 */
public class SPARQLWorker extends HttpWorker {

	private static final String XML_RESULT_ELEMENT_NAME = "result";
	private static final String XML_RESULT_ROOT_ELEMENT_NAME = "results";
	private static final String QUERY_RESULT_TYPE_JSON = "application/sparql-results+json";
	private static final String QUERY_RESULT_TYPE_XML = "application/sparql-results+xml";
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
		// bla
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

//	@Override
	private QueryExecutionStats executeQueryOld(String query, String queryID) {
		Instant start = Instant.now();
		final AtomicReference<String> res = new AtomicReference<String>("");

		try {
			// Execute Query
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
//			try (CloseableHttpClient client = HttpClients.createDefault();
//				 CloseableHttpResponse response = client.execute(request);) {
			try {
				HttpEntity entity = response.getEntity();
				int responseCode = response.getStatusLine().getStatusCode();
				if (responseCode != 200) {
					return new QueryExecutionStats(queryID, COMMON.QUERY_HTTP_FAILURE, durationInMilliseconds(start, Instant.now()));
//					return;
				}
				Header[] contentType = response.getHeaders("Content-Type");
				String cType = getContentTypeVal(contentType[0]);

//				this.processResult(queryID, start, entity, cType);

				executeAndTerminate(entity, res, cType);

				double duration = durationInMilliseconds(start, Instant.now());
				if (this.timeOut < duration) {
					return new QueryExecutionStats(queryID, 0L, duration);
//					return;
				}
				long size = 0L;
				if (QUERY_RESULT_TYPE_JSON.equals(cType)) {
					size = getJsonResultSize(res.get());
				} else if (QUERY_RESULT_TYPE_XML.equals(cType)) {
					size = getXmlResultSize(res.get());
				} else {
					size = StringUtils.countMatches(res.get(), "\n");
				}
				return new QueryExecutionStats(queryID, 1L, duration, size);
//				return;

			} catch (java.net.SocketTimeoutException | ConnectTimeoutException e) {
				System.out.println("Timeout occured for " + service + " - " + queryID);

				return new QueryExecutionStats(queryID, COMMON.QUERY_SOCKET_TIMEOUT, durationInMilliseconds(start, Instant.now()));
//				return;
			} catch (Exception e) {
				System.out.println("Query could not be exceuted: " + e);
				return new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now()));
//				return;
			} finally {
				response.close();
				client.close();
			}

		} catch (Exception e) {
			LOGGER.warn("Worker[{{}} : {{}}]: Could not execute the following query\n{{}}\n due to", this.workerType,
					this.workerID, query, e);
			return new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now()));
		}

	}

	@Override
	public QueryExecutionStats executeQuery(String query, String queryID) {
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

			return this.processResponse(queryID, start, client, response);

		} catch (Exception e) {
			LOGGER.warn("Worker[{{}} : {{}}]: Could not execute the following query\n{{}}\n due to", this.workerType,
					this.workerID, query, e);
			return new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now()));
		}

	}


	private QueryExecutionStats processResponse(String queryId, Instant startTime, CloseableHttpClient client, CloseableHttpResponse response) {

		// check if query execution took longer than timeout
		// TODO this is not necessary if result parsing is not considered in calculating duration. If so, can be removed.
		double duration = durationInMilliseconds(startTime, Instant.now());
		System.out.println("duration: " + duration);
		if (this.timeOut < duration) {
//			super.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_SOCKET_TIMEOUT, duration));
			return new QueryExecutionStats(queryId, COMMON.QUERY_SOCKET_TIMEOUT, duration);
		}

		int responseCode = response.getStatusLine().getStatusCode();
		if (responseCode != 200) {
//			super.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_HTTP_FAILURE, duration));
			return new QueryExecutionStats(queryId, COMMON.QUERY_HTTP_FAILURE, duration);
		}

		HttpEntity httpResponse = response.getEntity();
		Header[] contentTypeHeaders = response.getHeaders("Content-Type");
		String contentType = getContentTypeVal(contentTypeHeaders[0]);

		System.out.println("STATS " + queryId + " : " + httpResponse.getContentLength());

		try (InputStream inputStream = httpResponse.getContent();
			 BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			StringBuilder result = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				result.append(line);
			}
			System.out.println("[DEBUG]: byte size: " + result.length());

			long resultSize;
			if (QUERY_RESULT_TYPE_JSON.equals(contentType)) {
				resultSize = getJsonResultSize(result.toString());
			} else if (QUERY_RESULT_TYPE_XML.equals(contentType)) {
				resultSize = getXmlResultSize(result.toString());
			} else {
				resultSize = StringUtils.countMatches(result.toString(), "\n");
			}

			response.close();
			client.close();

			//			super.addResults(new QueryExecutionStats(queryId, 1L, duration, resultSize));
			return new QueryExecutionStats(queryId, 1L, duration, resultSize);


		} catch (IOException | ParseException | ParserConfigurationException | SAXException e) {
			System.out.println("Query results could not be parsed: " + e);
//			super.addResults(new QueryExecutionStats(queryId, COMMON.QUERY_UNKNOWN_EXCEPTION, duration));
			return new QueryExecutionStats(queryId, COMMON.QUERY_UNKNOWN_EXCEPTION, duration);
		}
	}


	private String getContentTypeVal(Header header) {
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

	private void executeAndTerminate(HttpEntity entity, AtomicReference<String> res, String cType)
			throws InterruptedException {
		ExecutorService service2 = Executors.newSingleThreadExecutor();
		service2.execute(new Runnable() {
			@Override
			public void run() {
				try (InputStream inputStream = entity.getContent();
					 BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
					StringBuilder result = new StringBuilder();
					String line;
					while ((line = br.readLine()) != null) {
						result.append(line);
					}
					System.out.println("[DEBUG]: byte size: " + result.length());
					res.set(result.toString());
					result = null;
				} catch (Exception e) {
					System.out.println("Query could not be exceuted: " + e);
				}
			}
		});

		service2.shutdown();
		service2.awaitTermination((long) (double) this.timeOut + 100, TimeUnit.MILLISECONDS);
	}

	private long getJsonResultSize(String res) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(res.toString().trim());
		long size = ((JSONArray) ((JSONObject) json.get("results")).get("bindings")).size();
		res = "";
		return size;
	}

	private long getXmlResultSize(String res) throws ParserConfigurationException, IOException, SAXException {
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
