package org.aksw.iguana.tp.tasks.impl.stresstest.worker;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.tp.config.CONSTANTS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Abstract Worker which will implement the runnable, the main loop,
 * the time to wait before a query and will send the results to the ResultProcessor module <br/>
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
	protected static final Logger LOGGER = LoggerFactory
		.getLogger(AbstractWorker.class);
    
	private boolean endSignal=false;
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
	protected int workerID;
	private Properties extra;

	private int fixedLatency;

	private int gaussianLatency;

	private Random latencyRandomizer;

	/**
	 * List which contains all Files representing one query(Pattern)
	 */
	protected File[] queryFileList;

	protected Long timeLimit;

	protected long startTime;

	private String queriesFileName;

	
	public AbstractWorker() {
	}
	
	public AbstractWorker(String taskID, int workerID, String workerType, Long timeLimit, String queriesFileName, Integer fixedLatency, Integer gaussianLatency) {
		 //Add task and Worker Specs
	    this.taskID = taskID;
	    this.workerID = workerID;
	    this.workerType = workerType;
	    this.timeLimit = timeLimit;

	    //workerID represents seed to be fair with different systems.
	    latencyRandomizer = new Random(this.workerID);
	    
	    //set Query file/folder Name
	    this.queriesFileName = queriesFileName;
	    
	    //Add latency Specs, add defaults
	    if(fixedLatency != null)
	    	this.fixedLatency = fixedLatency;
	    if(gaussianLatency != null)
	    	this.gaussianLatency = gaussianLatency;
	    LOGGER.debug("Initialized new Worker[{{}} : {{}}] for taskID {{}}", workerType, workerID, taskID);

	}
	
	@Override
	public void init(Properties p){
	    //Add task and Worker Specs
	    this.taskID = p.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY);
	    this.workerID = (int) p.get(CONSTANTS.WORKER_ID_KEY);
	    this.workerType = p.getProperty(CONSTANTS.WORKER_TYPE_KEY);
		this.timeLimit = (Long) p.get(CONSTANTS.TIME_LIMIT);


	    //workerID represents seed to be fair with different systems.
	    latencyRandomizer = new Random(this.workerID);
	    
	    //set Query file list
	    this.queryFileList = (File[]) p.get(CONSTANTS.QUERY_FILE_LIST);
	    
	    //Add latency Specs, add defaults
	    this.fixedLatency = (int) p.getOrDefault(CONSTANTS.FIXED_LATENCY, 0);
	    this.gaussianLatency = (int) p.getOrDefault(CONSTANTS.GAUSSIAN_LATENCY, 0);
	    LOGGER.debug("Initialized new Worker[{{}} : {{}}] for taskID {{}}", workerType, workerID, taskID);
	}
	
	@Override
	public void waitTimeMs(){
	    long wait=this.fixedLatency;
	    wait += Math.round((latencyRandomizer.nextGaussian()+1)*this.gaussianLatency);
	    LOGGER.debug("Worker[{{}} : {{}}]: Time to wait for next Query {{}}", workerType, workerID, wait);
	    try {
		Thread.sleep(wait);
	    } catch (InterruptedException e) {
		LOGGER.error("Worker[{{}} : {{}}]: Could not wait time before next query due to: {{}}", workerType, workerID, e);
		LOGGER.error("", e);
	    }
	}
	
	/**
	 * This will start the worker. 
	 * It will get the next query, wait as long as it should wait before executing the next query,
	 * then it will test the query and send it if not aborted yet to the ResultProcessor Module
	 *
	 */
	public void startWorker(){
	    	//set extra meta key to send late
	    	this.extra = new Properties();
	    	this.extra.put(CONSTANTS.WORKER_ID_KEY, workerID);
		this.extra.setProperty(CONSTANTS.WORKER_TYPE_KEY, workerType);
		//For Update and Logging purpose get startTime of Worker
		this.startTime = Calendar.getInstance().getTimeInMillis();
		
		LOGGER.info("Starting Worker[{{}} : {{}}].",this.workerType, this.workerID);
		//Execute Queries as long as the Stresstest will need.
		while(!this.endSignal){
			//Get next query
			StringBuilder query = new StringBuilder();
			StringBuilder queryID = new StringBuilder();
			try{
			    getNextQuery(query, queryID);
			    //check if endsignal was triggered
			    if(this.endSignal) {
			    	break;
			    }
			}catch(IOException e){
			    LOGGER.error("Worker[{{}} : {{}}] : Something went terrible wrong in getting the next query. Worker will be shut down.", this.workerType, this.workerID);
			    LOGGER.error("Error which occured:_", e);
			    break;
			}
			//Simulate Network Delay (or whatever should be simulated)
			waitTimeMs();
			//benchmark query
			long time = getTimeForQueryMs(query.toString(), queryID.toString());
			this.executedQueries++;
			//If endSignal was send during execution it should not be counted anymore.
			if(!this.endSignal){
				//create Properties store it in List
				Properties result = new Properties();
				result.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, this.taskID);
				result.put(COMMON.RECEIVE_DATA_TIME, time);
				result.put(COMMON.RECEIVE_DATA_SUCCESS, time>=0);
				result.setProperty(COMMON.QUERY_ID_KEY, queryID.toString());
				//Add extra Meta Key, worker ID and worker Type
				result.put(COMMON.EXTRA_META_KEY, this.extra);
				results.add(result);
			}
		}
		LOGGER.info("Stopping Worker[{{}} : {{}}].",this.workerType, this.workerID);
	}
	

	@Override
	public Collection<Properties> popQueryResults(){
		Collection<Properties> ret = this.results;
		this.results = new LinkedList<Properties>();
		return ret;
	}
	
	@Override
	public long getExecutedQueries(){
		return this.executedQueries;
	}
	
	@Override
	public void stopSending(){
		this.endSignal=true;
		LOGGER.debug("Worker[{{}} : {{}}] got stop signal.", workerType, workerID);
	}

	@Override
	public void run(){
		startWorker();
	}
	
	public String getQueriesFileName() {
		return this.queriesFileName;
	}
	
	public void setQueriesList(File[] queries) {
		this.queryFileList = queries;
	}

	public long getNoOfQueries() {
		return this.queryFileList.length;
	}
}
