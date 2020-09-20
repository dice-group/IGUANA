package org.aksw.iguana.cc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.cc.config.elements.*;
import org.aksw.iguana.commons.script.ScriptExecutor;
import org.aksw.iguana.cc.controller.TaskController;
import org.aksw.iguana.rp.controller.RPController;
import org.aksw.iguana.rp.metrics.Metric;
import org.aksw.iguana.rp.storage.Storage;
import org.apache.commons.exec.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Gets the {@link org.apache.commons.configuration.Configuration} component and will generate
 * a SuiteID and ExperimentIDs as well as TaskIDs for it.</br>
 * Afterwards it will execute a DataGenerator if specified and starts the taskProcessor with all specified tasks
 * <br/><br/>
 * The following order holds
 * <ol>
 * 	<li>For each Dataset</li>
 *  <li>For each Connection</li>
 *  <li>For each Task</li>
 * </ol>
 * 
 * 
 * @author f.conrads
 *
 */
public class IguanaConfig {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(IguanaConfig.class);

	private String suiteID;
	@JsonProperty(required = true)
	private List<Dataset> datasets;
	@JsonProperty(required = true)
	private List<Connection> connections;
	@JsonProperty(required = true)
	private List<Task> tasks;
	@JsonProperty(required = false)
	private String preScriptHook;
	@JsonProperty(required = false)
	private String postScriptHook;
	@JsonProperty(required = false)
	private List<MetricConfig> metrics;
	@JsonProperty(required = false)
	private List<StorageConfig> storages;


	/**
	 * starts the config
	 * @throws IOException 
	 * @throws ExecuteException 
	 */
	public void start() throws ExecuteException, IOException {
		initResultProcessor();
		TaskController controller = new TaskController();
		//get SuiteID
		String suiteID = generateSuiteID();
		//generate ExpID
		Integer expID = 0;

		for(Dataset dataset: datasets){
			expID++;
			Integer taskID = 0;
			for(Connection con : connections){
				for(Task task : tasks) {
					taskID++;
					String[] args = new String[] {dataset.getName(), con.getName(), taskID+""};
					if(preScriptHook!=null){
						LOGGER.info("Executing preScriptHook");
						ScriptExecutor.execSafe(preScriptHook, args);
					}
					LOGGER.info("Executing Task [{}: {}, {}, {}]", taskID, dataset.getName(), con.getName(), task.getClassName());
					controller.startTask(new String[]{suiteID, suiteID+"/"+expID.toString(), suiteID+"/"+expID.toString()+"/"+taskID.toString()}, dataset.getName(), con, task);
					if(postScriptHook!=null){
						LOGGER.info("Executing postScriptHook");
						ScriptExecutor.execSafe(postScriptHook, args);
					}
				}
			}
		}


	}

	private void initResultProcessor() {
		//If storage or metric is empty use default
		if(this.storages.isEmpty()){
			StorageConfig config = new StorageConfig();
			config.setClassName("NTFileStorage");
			storages.add(config);
		}
		if(this.metrics.isEmpty()){
			MetricConfig config = new MetricConfig();
			config.setClassName("QMPH");
			metrics.add(config);
			config = new MetricConfig();
			config.setClassName("QPS");
			metrics.add(config);
			config = new MetricConfig();
			config.setClassName("NoQPH");
			metrics.add(config);

		}
		//Create Storages
		List<Storage> storages = new ArrayList<Storage>();
		for(StorageConfig config : this.storages){
			storages.add(config.createStorage());
		}
		//Create Metrics
		List<Metric> metrics = new ArrayList<Metric>();
		for(MetricConfig config : this.metrics){
			metrics.add(config.createMetric());
		}
		RPController controller = new RPController();
		controller.init(storages, metrics);
	}


	private String generateSuiteID() {
		int currentTimeMillisHashCode = Math.abs(Long.valueOf(Instant.now().getEpochSecond()).hashCode());
		return String.valueOf(currentTimeMillisHashCode);
	}
	
}
