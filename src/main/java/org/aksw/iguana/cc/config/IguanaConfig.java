package org.aksw.iguana.cc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.cc.config.elements.*;
import org.aksw.iguana.cc.controller.TaskController;
import org.aksw.iguana.cc.tasks.impl.Stresstest;
import org.aksw.iguana.commons.script.ScriptExecutor;
import org.aksw.iguana.rp.controller.RPController;
import org.aksw.iguana.rp.metrics.Metric;
import org.aksw.iguana.rp.metrics.impl.*;
import org.aksw.iguana.rp.storage.Storage;
import org.apache.commons.exec.ExecuteException;
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
 * a SuiteID and ExperimentIDs as well as TaskIDs for it.<br/>
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
 * @author f.conrads
 *
 */
public class IguanaConfig {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(IguanaConfig.class);

	@JsonProperty(required = true)
	private List<DatasetConfig> datasets;
	@JsonProperty(required = true)
	private List<ConnectionConfig> connections;
	@JsonProperty(required = true)
	private List<Stresstest.Config> tasks;
	@JsonProperty
	private String preScriptHook;
	@JsonProperty
	private String postScriptHook;
	@JsonProperty
	private List<MetricConfig> metrics;
	@JsonProperty
	private List<StorageConfig> storages;


	/**
	 * starts the config
	 * @throws IOException 
	 * @throws ExecuteException 
	 */
	public void start() throws ExecuteException, IOException {
		RPController rpController = initResultProcessor();
		TaskController controller = new TaskController();
		//get SuiteID
		String suiteID = generateSuiteID();
		//generate ExpID
		int expID = 0;

		for(DatasetConfig dataset: datasets){
			expID++;
			Integer taskID = 0;
			for(ConnectionConfig con : connections){
				for(Stresstest.Config task : tasks) {
					taskID++;
					String[] args = new String[] {};
					if(preScriptHook!=null){
						LOGGER.info("Executing preScriptHook");
						String execScript = preScriptHook.replace("{{dataset.name}}", dataset.name())
								.replace("{{connection}}", con.name())
								.replace("{{connection.version}}", con.version())
								.replace("{{taskID}}", taskID+"");
						LOGGER.info("Finished preScriptHook");
						if(dataset.file()!=null){
							execScript = execScript.replace("{{dataset.file}}", dataset.file());
						}

						ScriptExecutor.execSafe(execScript, args);
					}

				}
			}
		}
		rpController.close();

		LOGGER.info("Finished benchmark");
	}

	private RPController initResultProcessor() {
		//If storage or metric is empty use default
		if(this.storages== null || this.storages.isEmpty()){
			storages = new ArrayList<>();
		}
		if(this.metrics == null || this.metrics.isEmpty()){
			LOGGER.info("No metrics were set. Using default metrics.");
			metrics = new ArrayList<>();
			MetricConfig config = new MetricConfig();
			config.setClassName(QMPHMetric.class.getCanonicalName());
			metrics.add(config);
			config = new MetricConfig();
			config.setClassName(QPSMetric.class.getCanonicalName());
			Map<String, Object> configMap = new HashMap<>();
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
		List<Storage> storages = new ArrayList<>();
		for(StorageConfig config : this.storages){
		}
		//Create Metrics
		List<Metric> metrics = new ArrayList<>();
		for(MetricConfig config : this.metrics){
			metrics.add(config.createMetric());
		}
		RPController controller = new RPController();
		controller.init(storages, metrics);
		return controller;
	}


	private String generateSuiteID() {
		int currentTimeMillisHashCode = Math.abs(Long.valueOf(Instant.now().getEpochSecond()).hashCode());
		return String.valueOf(currentTimeMillisHashCode);
	}
	
}
