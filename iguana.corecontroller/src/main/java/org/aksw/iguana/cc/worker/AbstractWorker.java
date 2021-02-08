package org.aksw.iguana.cc.worker;

import org.aksw.iguana.cc.config.CONSTANTS;
import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.query.set.QuerySet;
import org.aksw.iguana.cc.utils.FileUtils;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.constants.COMMON;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;


/**
 * The Abstract Worker which will implement the runnable, the main loop, the
 * time to wait before a query and will send the results to the ResultProcessor
 * module <br/>
 * so the Implemented Workers only need to implement which query to test next
 * and how to test this query.
 *
 * @author f.conrads
 *
 */
public abstract class AbstractWorker implements Worker {

	/**
	 * Logger which should be used
	 */
	protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorker.class);

	protected boolean endSignal = false;
	protected long executedQueries;

	private Collection<Properties> results = new LinkedList<Properties>();
	protected String taskID;

	/**
	 * The worker Type. f.e. SPARQL or UPDATE or SQL or whatever
	 */
	protected String workerType;
	/**
	 * The unique ID of the worker, should be from 0 to n
	 */
	protected Integer workerID;
	protected Properties extra = new Properties();

	private Integer fixedLatency=0;

	private Integer gaussianLatency=0;

	private Random latencyRandomizer;

	/**
	 * List which contains all Files representing one query(Pattern)
	 */
	protected QuerySet[] queryFileList;

	protected Double timeLimit;

	protected Instant startTime;

	protected String queriesFileName;

	protected Connection con;

	protected Double timeOut=180000D;

	protected int queryHash;

	public AbstractWorker(String taskID, Connection connection, String queriesFile, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, String workerType, Integer workerID) {
		this.taskID=taskID;
		this.workerID = workerID;
		this.workerType = workerType;
		this.con = connection;
		if (timeLimit != null){
			this.timeLimit = timeLimit.doubleValue();
		}
		latencyRandomizer = new Random(this.workerID);
		if(timeOut!=null)
			this.timeOut = timeOut.doubleValue();
		// Add latency Specs, add defaults
		if(fixedLatency!=null)
			this.fixedLatency = fixedLatency;
		if(gaussianLatency!=null)
			this.gaussianLatency = gaussianLatency;
		// set Query file/folder Name
		this.queriesFileName = queriesFile;
		LOGGER.debug("Initialized new Worker[{{}} : {{}}] for taskID {{}}", workerType, workerID, taskID);
	}


	@Override
	public void waitTimeMs() {
		Double wait = this.fixedLatency.doubleValue();
		double gaussian = latencyRandomizer.nextDouble();
		wait += (gaussian * 2) * this.gaussianLatency;
		LOGGER.debug("Worker[{} : {}]: Time to wait for next Query {}", workerType, workerID, wait);
		try {
			if(wait>0)
				Thread.sleep(wait.intValue());
		} catch (InterruptedException e) {
			LOGGER.error("Worker[{{}} : {}]: Could not wait time before next query due to: {}", workerType,
					workerID, e);
		}
	}

	/**
	 * This will start the worker. It will get the next query, wait as long as it
	 * should wait before executing the next query, then it will test the query and
	 * send it if not aborted yet to the ResultProcessor Module
	 *
	 */
	public void startWorker() {
		// set extra meta key to send late
		this.extra = new Properties();
		this.extra.put(CONSTANTS.WORKER_ID_KEY, workerID);
		this.extra.setProperty(CONSTANTS.WORKER_TYPE_KEY, workerType);
		if(this.queryFileList!=null)
			this.extra.put(COMMON.NO_OF_QUERIES, this.queryFileList.length);
		// For Update and Logging purpose get startTime of Worker
		this.startTime = Instant.now();

		this.queryHash = FileUtils.getHashcodeFromFileContent(this.queriesFileName);

		LOGGER.info("Starting Worker[{{}} : {{}}].", this.workerType, this.workerID);
		// Execute Queries as long as the Stresstest will need.
		while (!this.endSignal) {
			// Get next query
			StringBuilder query = new StringBuilder();
			StringBuilder queryID = new StringBuilder();
			try {
				getNextQuery(query, queryID);
				// check if endsignal was triggered
				if (this.endSignal) {
					break;
				}
			} catch (IOException e) {
				LOGGER.error(
						"Worker[{{}} : {{}}] : Something went terrible wrong in getting the next query. Worker will be shut down.",
						this.workerType, this.workerID);
				LOGGER.error("Error which occured:_", e);
				break;
			}
			// Simulate Network Delay (or whatever should be simulated)
			waitTimeMs();

			// benchmark query
			try {
				executeQuery(query.toString(), queryID.toString());
			} catch (Exception e) {
				LOGGER.error("Worker[{{}} : {{}}] : ERROR with query: {{}}", this.workerType, this.workerID,
						query.toString());
			}
			//this.executedQueries++;
		}
		LOGGER.info("Stopping Worker[{{}} : {{}}].", this.workerType, this.workerID);
	}

	protected HttpContext getAuthContext(String endpoint){
		HttpClientContext context = HttpClientContext.create();

		if(con.getPassword()!=null && con.getUser()!=null && !con.getPassword().isEmpty() && !con.getUser().isEmpty()) {
			CredentialsProvider provider = new BasicCredentialsProvider();

			provider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
					new UsernamePasswordCredentials(con.getUser(), con.getPassword()));

			//create target host
			String targetHost = endpoint;
			try {
				URI uri = new URI(endpoint);
				targetHost = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			//set Auth cache
			AuthCache authCache = new BasicAuthCache();
			BasicScheme basicAuth = new BasicScheme();
			authCache.put(HttpHost.create(targetHost), basicAuth);

			context.setCredentialsProvider(provider);
			context.setAuthCache(authCache);

		}
		return context;
	}

	public synchronized void addResults(QueryExecutionStats results)
	{
		if (!this.endSignal) {
			// create Properties store it in List
			Properties result = new Properties();
			result.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, this.taskID);
			result.put(COMMON.RECEIVE_DATA_TIME, results.getExecutionTime());
			result.put(COMMON.RECEIVE_DATA_SUCCESS, results.getResponseCode());
			result.put(COMMON.RECEIVE_DATA_SIZE, results.getResultSize());
			result.put(COMMON.QUERY_HASH, queryHash);
			result.setProperty(COMMON.FULL_QUERY_ID_KEY, results.getQueryID());
			result.put(COMMON.PENALTY, this.timeOut);
			// Add extra Meta Key, worker ID and worker Type
			result.put(COMMON.EXTRA_META_KEY, this.extra);
			setResults(result);
			executedQueries++;

		}
	}

	protected synchronized void setResults(Properties result) {
		results.add(result);
	}

	@Override
	public synchronized Collection<Properties> popQueryResults() {
		if(results.isEmpty()){
			return null;
		}
		Collection<Properties> ret = this.results;
		this.results = new LinkedList<Properties>();
		return ret;
	}

	@Override
	public long getExecutedQueries() {
		return this.executedQueries;
	}

	@Override
	public void stopSending() {
		this.endSignal = true;
		LOGGER.debug("Worker[{{}} : {{}}] got stop signal.", workerType, workerID);
	}

	@Override
	public boolean isTerminated(){
		return this.endSignal;
	}


	@Override
	public void run() {
		startWorker();
	}

	/**
	 * Returns the name of the queries file name/update path
	 *
	 * @return file name/update path
	 */
	public String getQueriesFileName() {
		return this.queriesFileName;
	}

	/**
	 * Sets the Query Instances repr. in Files.
	 *
	 * @param queries
	 *            File containing the query instances.
	 */
	public void setQueriesList(QuerySet[] queries) {
		this.queryFileList = queries;
	}

	/**
	 * The number of Queries in one mix
	 *
	 * @return
	 */
	public long getNoOfQueries() {
		return this.queryFileList.length;
	}

	@Override
	public boolean hasExecutedNoOfQueryMixes(double noOfQueryMixes){
		return getExecutedQueries() / (getNoOfQueries() * 1.0) >= noOfQueryMixes;
	}
}
