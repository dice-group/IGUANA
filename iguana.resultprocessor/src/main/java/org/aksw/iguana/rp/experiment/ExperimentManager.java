package org.aksw.iguana.rp.experiment;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.metrics.MetricManager;
import org.aksw.iguana.rp.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The ExperimentManager manages the incoming properties from the 
 * tasks and sort them to the correct experiments
 * One Experiment is simply a {@link org.aksw.iguana.rp.metrics.MetricManager}
 * 
 * @author f.conrads
 */
public class ExperimentManager {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ExperimentManager.class);
	
	private Map<String, MetricManager> experiments = new HashMap<String, MetricManager>();
	private MetricManager globalMetricManager;

	private StorageManager storageManager;

	private static ExperimentManager instance;

	public synchronized static ExperimentManager getInstance(){
		if (instance == null) {
			instance = new ExperimentManager(MetricManager.getInstance(), StorageManager.getInstance());
		}
		return instance;
	}


	/**
	 * Initialize the ExperimentManager with the global {@link org.aksw.iguana.rp.metrics.MetricManager}
	 * @param globalMetricManager
	 */
	public ExperimentManager(MetricManager globalMetricManager, StorageManager storageManager){
		this.globalMetricManager = globalMetricManager;
		this.storageManager = storageManager;
	}
	
	/**
	 * 
	 * @param p
	 */
	public void receiveData(Properties p){
		//check if start, content, end 
		if(p.containsKey(COMMON.RECEIVE_DATA_START_KEY)){
			startExperimentTask(p);
		}
		else if(p.containsKey(COMMON.RECEIVE_DATA_END_KEY)){
			endExperimentTask(p);
		}
		else{
			content(p);
		}
	}
	
	/**
	 * This will start an experiment. This will initialize the following things
	 * Queries, Metrics, Resultsizes, Workers, Tasks, Suite(?), metricsManager
	 * 
	 * @param p
	 */
	private void startExperimentTask(Properties p){
		//Check if properties contains an experiment ID, if not do nothing.
		if(!p.containsKey(COMMON.EXPERIMENT_TASK_ID_KEY)){
			LOGGER.error("Could not find experiment task ID in properties.");
			LOGGER.error("Will ignore this properties object {}", p.toString());
			return;
		}
		//Get the Experiment task ID 
		String taskID =  p.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY);
		LOGGER.info("Got start flag for experiment task ID {}", taskID);

		
		//Add metricManager to experiments
		experiments.put(taskID, globalMetricManager);

		globalMetricManager.addMetaData(p);
		//check all the properties. (Queries, Results, Workers) and add them to the Storages
		storageManager.addMetaData(p);
		LOGGER.info("Will start experiment task with ID {} now.", taskID);
	}
	
	/**
	 * Will sort the properties to the correct experiment according to their IDs
	 * It will simply add the properties to the {@link org.aksw.iguana.rp.metrics.MetricManager}
	 * @param p
	 */
	private void content(Properties p){
		String taskID = p.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY);
		LOGGER.debug("Got content for experiment task ID: {} ", taskID);
		if(experiments.containsKey(taskID))
			experiments.get(taskID).receiveData(p);
		else
			LOGGER.warn("Got content for experiment task ID: {} but task never start", taskID);
	}
	
	/**
	 * This will end the experiment and start the close method of the associated metrics
	 * @param p
	 */
	private void endExperimentTask(Properties p){
		String taskID = p.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY);
		storageManager.endTask(taskID);
		LOGGER.info("Got end Flag for experiment task ID {}", taskID);
		if(experiments.containsKey(taskID)){
			experiments.get(taskID).close();
			experiments.remove(taskID);
		}
		else{
			LOGGER.warn("Could not find Experiment Task with ID: {}.", taskID);
		}
		storageManager.commit();
	}
}
