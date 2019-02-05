package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import java.io.File;
import java.io.IOException;
import java.lang.Exception;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
//import org.json.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.config.RequestConfig;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.tp.utils.FileUtils;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * 
 * A Worker using SPARQL 1.1 to create service request.
 * 
 * @author f.conrads
 *
 */
public class SPARQLWorker extends AbstractWorker {

	private int currentQueryID = 0;

	private Random queryPatternChooser;

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
	public Long[] getTimeForQueryMs(String query, String queryID) {
		QueryExecution exec = QueryExecutionFactory.sparqlService(service, query);
		// exec.setTimeout(this.timeOut);
		// Set query timeout
		exec.setTimeout(this.timeOut, TimeUnit.MILLISECONDS, this.timeOut, TimeUnit.MILLISECONDS);
		long start = System.currentTimeMillis();

		try {
			// Execute Query
			System.out.println("D3.1: " + this.timeOut);
			String qEncoded = URLEncoder.encode(query);
			String addChar = "?";
			if(service.contains("?")) {
				addChar = "&";
			}
			String url = service + addChar + "query=" + qEncoded;
			CloseableHttpClient client = HttpClients.createDefault();
			CloseableHttpResponse response = null;
			final AtomicReference<String> res = new AtomicReference<String>("");

			try {
				HttpGet request = new HttpGet(url);
				RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeOut.intValue())
						.setConnectTimeout(timeOut.intValue()).build();

				request.setConfig(requestConfig);
				
				response = client.execute(request);
				HttpEntity entity = response.getEntity();
				int responseCode = response.getStatusLine().getStatusCode();
				System.out.println("\nSending 'GET' request to URL : " + service);
				System.out.println("Response Code : " + responseCode);
				if (responseCode != 200) {
					// return new Long[]{-1L, 0L};
					return new Long[] { COMMON.WRONG_RESPONSE_CODE_VALUE, System.currentTimeMillis() - start };

				}
				ExecutorService service2 = Executors.newSingleThreadExecutor();
				// ResultSet res;
				service2.execute(new Runnable() {
					@Override
					public void run() {

						try (InputStream inputStream = entity.getContent();) {
							BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
							StringBuilder result = new StringBuilder();
							String line;
							int size = 0;
							while ((line = br.readLine()) != null) {
								result.append(line).append("\n");
								size += result.length();
							}
							res.set(result.toString());
							result = new StringBuilder();
//							System.out.println("[DEBUG SIZE 1] "+res.get());
//							System.out.println("[DEBUG SIZE 1.2] "+result.toString());
							System.out.println("ResSize:\t" + service + "\t" + queryID + "\t" + size);
						} catch (Exception e) {
							System.out.println("Query could not be exceuted: " + e);
						}
					}
				});
				// response.close();
//		JSONObject json = new JSONObject(result.toString());
//		int size = json.getJSONObject("results").getJSONArray("bindings").length();
//					ResultSet res = exec.execSelect();
//					int size = ResultSetFormatter.consume(res);
//					int size=result.length();
//				}
//			});
				service2.shutdown();
				long endS = System.currentTimeMillis();

				service2.awaitTermination(this.timeOut + 100, TimeUnit.MILLISECONDS);
				// check ResultSet.
//			exec.close();
			} catch (java.net.SocketTimeoutException | ConnectTimeoutException e ) {
				System.out.println("Timeout occured for " + service + " - " + queryID);

				return new Long[] { COMMON.SOCKET_TIMEOUT_VALUE, System.currentTimeMillis() - start };

			} catch (Exception e) {
				System.out.println("Query could not be exceuted: " + e);
				return new Long[] { COMMON.UNKNOWN_EXCEPTION_VALUE, System.currentTimeMillis() - start };
			} finally {
				if (response != null)
					response.close();
				client.close();

			}
			System.out.println("D3.3");
			long end = System.currentTimeMillis();
			if (this.timeOut < end - start) {
				return new Long[] { 0L, System.currentTimeMillis() - start };
			}
			// LOGGER.debug("Worker[{{}} : {{}}]: Query with ID {{}} took {{}} and has {{}}
			// results.", this.workerType,
			// this.workerID, queryID, end - start, size);
			// Return time
			
			try {
				JSONParser parser = new JSONParser();
				JSONObject json = (JSONObject) parser.parse(res.toString().trim());
				long size = ((JSONArray)((JSONObject)json.get("results")).get("bindings")).size();
				System.out.println("Size debug: "+size);
				res.set("");
				return new Long[] { 1L, end - start , size};
			}catch(Exception e) {
				e.printStackTrace();
			}
			System.out.println("Size debug: -1");
			return new Long[] { 1L, end - start};
		} catch (Exception e) {
			LOGGER.warn("Worker[{{}} : {{}}]: Could not execute the following query\n{{}}\n due to", this.workerType,
					this.workerID, query, e);
		}
		// Exception was thrown, return error
		// return -1L;
		return new Long[] { COMMON.UNKNOWN_EXCEPTION_VALUE, System.currentTimeMillis() - start };

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
