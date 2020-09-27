package org.aksw.iguana.cc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.cc.config.elements.*;
import org.aksw.iguana.cc.controller.TaskController;
import org.aksw.iguana.commons.script.ScriptExecutor;
import org.aksw.iguana.rp.controller.RPController;
import org.aksw.iguana.rp.metrics.Metric;
import org.aksw.iguana.rp.metrics.impl.*;
import org.aksw.iguana.rp.storage.Storage;
import org.aksw.iguana.rp.storage.impl.NTFileStorage;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gets either a JSON or YAML configuration file using a json schema and will generate
 * a SuiteID and ExperimentIDs as well as TaskIDs for it.</br>
 * Afterwards it will start the taskProcessor with all specified tasks
 * <br/><br/>
 * The following order holds
 * <ol>
 * 	<li>For each Dataset</li>
 *  <li>For each Connection</li>
 *  <li>For each Task</li>
 * </ol>
 *
 * Further on executes the pre and post script hooks, before and after a class.
 * Following values will be exchanged in the script string {{Connection}} {{Dataset.name}} {{Dataset.file}} {{taskID}}
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
					String[] args = new String[] {};
					if(preScriptHook!=null){
						LOGGER.info("Executing preScriptHook");
						String execScript = preScriptHook.replace("{{dataset.name}}", dataset.getName())
								.replace("{{connection}}", con.getName())
								.replace("{{taskID}}", taskID+"");
						if(dataset.getFile()!=null){
							execScript = execScript.replace("{{dataset.file}}", dataset.getFile());
						}

						ScriptExecutor.execSafe(execScript, args);
					}
					LOGGER.info("Executing Task [{}: {}, {}, {}]", taskID, dataset.getName(), con.getName(), task.getClassName());
					controller.startTask(new String[]{suiteID, suiteID+"/"+expID.toString(), suiteID+"/"+expID.toString()+"/"+taskID.toString()}, dataset.getName(), SerializationUtils.clone(con), SerializationUtils.clone(task));
					if(postScriptHook!=null){
						LOGGER.info("Executing postScriptHook");
						String execScript = postScriptHook.replace("{{dataset.name}}", dataset.getName())
								.replace("{{connection}}", con.getName())
								.replace("{{taskID}}", taskID+"");
						if(dataset.getFile()!=null){
							execScript = execScript.replace("{{dataset.file}}", dataset.getFile());
						}
						ScriptExecutor.execSafe(execScript, args);
					}
				}
			}
		}


	}

	private void initResultProcessor() {
		//If storage or metric is empty use default
		if(this.storages== null || this.storages.isEmpty()){
			storages = new ArrayList<>();
			StorageConfig config = new StorageConfig();
			config.setClassName(NTFileStorage.class.getCanonicalName());
			storages.add(config);
		}
		if(this.metrics == null || this.metrics.isEmpty()){
			LOGGER.info("No metrics were set. Using default metrics.");
			metrics = new ArrayList<>();
			MetricConfig config = new MetricConfig();
			config.setClassName(QMPHMetric.class.getCanonicalName());
			metrics.add(config);
			config = new MetricConfig();
			config.setClassName(QPSMetric.class.getCanonicalName());
			Map<Object, Object> configMap = new HashMap<Object, Object>();
			configMap.put("penalty", 180000);
			config.setConfiguration(configMap);
			metrics.add(config);
			config = new MetricConfig();
			config.setClassName(NoQPHMetric.class.getCanonicalName());
			metrics.add(config);
			config = new MetricConfig();
			config.setClassName(AvgQPSMetric.class.getCanonicalName());
			metrics.add(config);
			config = new MetricConfig();
			config.setClassName(NoQMetric.class.getCanonicalName());
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
