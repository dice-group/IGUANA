package org.aksw.iguana.tp.tasks.impl.stresstest.worker;

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

	private static final Logger LOGGER = LoggerFactory
		.getLogger(AbstractWorker.class);
    
	private boolean endSignal=false;
	private long executedQueries;
	
	private Collection<Properties> results = new LinkedList<Properties>();
	private String taskID;
	
	private String workerType;
	private int workerID;
	private Properties extra;

	private LatencyStrategy latencyStrategy;
	private int latencyBaseValue;
	
	
	
	@Override
	public void init(Properties p){
	    //Add task and Worker Specs
	    this.taskID = p.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY);
	    this.workerID = (int) p.get(CONSTANTS.WORKER_ID_KEY);
	    this.workerType = p.getProperty(CONSTANTS.WORKER_TYPE_KEY);
	    //Add latency Specs, add defaults
	    this.latencyStrategy = LatencyStrategy.valueOf(p.getProperty(CONSTANTS.LATENCY_STRATEGY, "NONE"));
	    this.latencyBaseValue = (int) p.getOrDefault(CONSTANTS.LATENCY_BASE_VALUE, 0);
	    LOGGER.debug("Initialized new Worker[{{}} : {{}}] for taskID {{}}", workerType, workerID, taskID);
	}
	
	@Override
	public void waitTimeMs(){
	    long wait=0;
	    switch(latencyStrategy){
	    case NONE:
		return;
	    case FIXED:
		wait = this.latencyBaseValue;
		break;
	    case VARIABLE:
		//workerID represents seed to be fair with different systems.
		Random rand = new Random(this.workerID);
		wait = Math.round((rand.nextGaussian()+1)*this.latencyBaseValue);
		break;
	    }
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
		LOGGER.info("Starting Worker[{{}} : {{}}].",this.workerType, this.workerID);
		//Execute Queries as long as the Stresstest will need.
		while(!this.endSignal){
			//Get next query
			StringBuilder query = new StringBuilder();
			StringBuilder queryID = new StringBuilder();
			getNextQuery(query, queryID);
			//Simulate Network Delay (or whatever should be simulated)
			waitTimeMs();
			//benchmark query
			int time = getTimeForQueryMs(query.toString());
			LOGGER.debug("Executed Query with ID {{}} in {{}} ms", queryID, time);
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

}
