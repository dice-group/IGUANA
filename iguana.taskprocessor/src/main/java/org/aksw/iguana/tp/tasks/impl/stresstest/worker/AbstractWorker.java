package org.aksw.iguana.tp.tasks.impl.stresstest.worker;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.tp.config.CONSTANTS;
import org.aksw.iguana.tp.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
	private String taskID;

	/**
	 * The worker Type. f.e. SPARQL or UPDATE or SQL or whatever
	 */
	protected String workerType;
	/**
	 * The unique ID of the worker, should be from 0 to n
	 */
	protected Integer workerID;
	protected Properties extra;

	private Integer fixedLatency=0;

	private Integer gaussianLatency=0;

	private Random latencyRandomizer;

	/**
	 * List which contains all Files representing one query(Pattern)
	 */
	protected File[] queryFileList;

	protected Long timeLimit;

	protected long startTime;

	protected String queriesFileName;

	protected String service;
	
	protected String user;
	protected String password;

	protected Long timeOut=180000L;

	private long noOfQueryMixes;

	/**
	 * Needs to be called if init is used
	 * @param workerType 
	 */
	public AbstractWorker(String workerType) {
		// needs
		this.workerType=workerType;
	}

	/**
	 * Default Constructor
	 * 
	 * @param args
	 * @param workerType
	 */
	public AbstractWorker(String[] args, String workerType) {
		this(workerType);
		init(args);
	}


	@Override
	public void init(String[] args) {
		// Add task and Worker Specs
		this.taskID = args[0];
		this.workerID = Integer.parseInt(args[1]);

		if(args[2]!=null)
			this.timeLimit = Long.parseLong(args[2]);
		this.service = args[3];
		this.user=args[4];
		this.password=args[5];
		if(args[6]!=null)
			this.timeOut = Long.parseLong(args[6]);
		// workerID represents seed to be fair with different systems.
		latencyRandomizer = new Random(this.workerID);

		// set Query file/folder Name
		this.queriesFileName = args[7];

		// Add latency Specs, add defaults
		if(args[6]!=null)
			this.fixedLatency = Integer.parseInt(args[8]);
		if(args[7]!=null)
			this.gaussianLatency = Integer.parseInt(args[9]);
		
		LOGGER.debug("Initialized new Worker[{{}} : {{}}] for taskID {{}}", workerType, workerID, taskID);

	}

	@Override
	public void init(Properties p) {
		// Add task and Worker Specs
		this.taskID = p.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY);
		this.workerID = Integer.parseInt(p.getProperty(CONSTANTS.WORKER_ID_KEY));

		if(this.workerType == null)
			this.workerType = p.getProperty(CONSTANTS.WORKER_TYPE_KEY);

		if(p.containsKey(CONSTANTS.TIME_LIMIT))
			this.timeLimit = Long.parseLong(p.getProperty(CONSTANTS.TIME_LIMIT));

		if(p.containsKey(CONSTANTS.SPARQL_TIMEOUT))
			this.timeOut = Long.parseLong(p.getProperty(CONSTANTS.SPARQL_TIMEOUT));

		this.service = p.getProperty(CONSTANTS.SERVICE_ENDPOINT);
		this.user = p.getProperty(CONSTANTS.USERNAME);
		this.password = p.getProperty(CONSTANTS.PASSWORD);

		// workerID represents seed to be fair with different systems.
		latencyRandomizer = new Random(this.workerID);

		// set Query file/folder Name
		this.queriesFileName = p.getProperty(CONSTANTS.QUERIES_FILE_NAME);

		// Add latency Specs, add defaults
		if(p.containsKey(CONSTANTS.FIXED_LATENCY))
			this.fixedLatency = Integer.parseInt(p.getProperty(CONSTANTS.FIXED_LATENCY));

		if(p.containsKey(CONSTANTS.GAUSSIAN_LATENCY))
			this.gaussianLatency = Integer.parseInt(p.getProperty(CONSTANTS.GAUSSIAN_LATENCY));

		LOGGER.debug("Initialized new Worker[{{}} : {{}}] for taskID {{}}", workerType, workerID, taskID);
	}

	@Override
	public void waitTimeMs() {
		long wait = this.fixedLatency;
		wait += Math.round((latencyRandomizer.nextGaussian() + 1) * this.gaussianLatency);
		LOGGER.debug("Worker[{{}} : {{}}]: Time to wait for next Query {{}}", workerType, workerID, wait);
		try {
			System.out.println("Wait: "+wait);
			if(wait>0)	
				Thread.sleep(wait);
		} catch (InterruptedException e) {
			LOGGER.error("Worker[{{}} : {{}}]: Could not wait time before next query due to: {{}}", workerType,
					workerID, e);
			LOGGER.error("", e);
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
		this.startTime = Calendar.getInstance().getTimeInMillis();

		int queryHash = FileUtils.getHashcodeFromFileContent(this.queriesFileName);

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
			Long time = 0L;
			Long[] resultTime = new Long[]{0L, 0L};
			try {
				resultTime = getTimeForQueryMs(query.toString(), queryID.toString());
				time = resultTime[1];
			} catch (Exception e) {
				LOGGER.error("Worker[{{}} : {{}}] : ERROR with query: {{}}", this.workerType, this.workerID,
						query.toString());
				time = -1L;
			}
			this.executedQueries++;
			// If endSignal was send during execution it should not be counted anymore.
			if (!this.endSignal) {
				// create Properties store it in List
				Properties result = new Properties();
				result.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, this.taskID);
				result.put(COMMON.RECEIVE_DATA_TIME, time);
				result.put(COMMON.RECEIVE_DATA_SUCCESS, resultTime[0]);
				if(resultTime.length>2) {
					result.put(COMMON.RECEIVE_DATA_SIZE, resultTime[2]);
				}
				result.put(COMMON.QUERY_HASH, queryHash);
				result.setProperty(COMMON.QUERY_ID_KEY, queryID.toString());
				// Add extra Meta Key, worker ID and worker Type
				result.put(COMMON.EXTRA_META_KEY, this.extra);
				setResults(result);
			}
		}
		LOGGER.info("Stopping Worker[{{}} : {{}}].", this.workerType, this.workerID);
	}

	private synchronized void setResults(Properties result) {
		results.add(result);
	}

	@Override
	public synchronized Collection<Properties> popQueryResults() {
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
	public void setQueriesList(File[] queries) {
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
}
