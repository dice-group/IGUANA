package org.aksw.iguana.cc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.cc.config.elements.*;
import org.aksw.iguana.cc.controller.TaskController;
import org.aksw.iguana.cc.tasks.stresstest.metrics.Metric;
import org.aksw.iguana.cc.tasks.stresstest.metrics.MetricManager;
import org.aksw.iguana.cc.tasks.stresstest.metrics.impl.*;
import org.aksw.iguana.cc.tasks.stresstest.storage.StorageManager;
import org.aksw.iguana.commons.script.ScriptExecutor;
import org.aksw.iguana.cc.tasks.stresstest.storage.Storage;
import org.aksw.iguana.cc.tasks.stresstest.storage.impl.NTFileStorage;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(IguanaConfig.class);

	@JsonProperty(required = true)
	private List<DatasetConfig> datasets;
	@JsonProperty(required = true)
	private List<ConnectionConfig> connections;
	@JsonProperty(required = true)
	private List<TaskConfig> tasks;
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
		initResultProcessor();
		TaskController controller = new TaskController();
		//get SuiteID
		String suiteID = generateSuiteID();
		//generate ExpID
		int expID = 0;

		for(DatasetConfig dataset: datasets){
			expID++;
			Integer taskID = 0;
			for(ConnectionConfig con : connections){
				for(TaskConfig task : tasks) {
					taskID++;
					String[] args = new String[] {};
					if(preScriptHook!=null){
						LOGGER.info("Executing preScriptHook");
						String execScript = preScriptHook.replace("{{dataset.name}}", dataset.getName())
								.replace("{{connection}}", con.getName())
								.replace("{{connection.version}}", con.getVersion("{{connection.version}}"))
								.replace("{{taskID}}", taskID+"");
						LOGGER.info("Finished preScriptHook");
						if(dataset.getFile()!=null){
							execScript = execScript.replace("{{dataset.file}}", dataset.getFile());
						}

						ScriptExecutor.execSafe(execScript, args);
					}
					LOGGER.info("Executing Task [{}/{}: {}, {}, {}]", taskID, task.getName(), dataset.getName(), con.getName(), task.getClassName());
					controller.startTask(new String[]{suiteID, suiteID + "/" + expID, suiteID + "/" + expID + "/" + taskID}, dataset.getName(), SerializationUtils.clone(con), SerializationUtils.clone(task));
					if(postScriptHook!=null){
						String execScript = postScriptHook.replace("{{dataset.name}}", dataset.getName())
								.replace("{{connection}}", con.getName())
								.replace("{{connection.version}}", con.getVersion("{{connection.version}}"))
								.replace("{{taskID}}", taskID+"");
						if(dataset.getFile()!=null){
							execScript = execScript.replace("{{dataset.file}}", dataset.getFile());
						}
						LOGGER.info("Executing postScriptHook {}", execScript);
						ScriptExecutor.execSafe(execScript, args);
						LOGGER.info("Finished postScriptHook");
					}
				}
			}
		}

		LOGGER.info("Finished benchmark");
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
			config.setClassName(QMPH.class.getCanonicalName());
			metrics.add(config);
			config = new MetricConfig();
			config.setClassName(QPS.class.getCanonicalName());
			metrics.add(config);
			config = new MetricConfig();
			config.setClassName(NoQPH.class.getCanonicalName());
			metrics.add(config);
			config = new MetricConfig();
			config.setClassName(AvgQPS.class.getCanonicalName());
			metrics.add(config);
			config = new MetricConfig();
			config.setClassName(NoQ.class.getCanonicalName());
			metrics.add(config);
			config = new MetricConfig();
			config.setClassName(AggregatedExecutionStatistics.class.getCanonicalName());
			metrics.add(config);
		}

		//Create Storages
		List<Storage> storages = new ArrayList<>();
		for(StorageConfig config : this.storages){
			storages.add(config.createStorage());
		}
		//Create Metrics
		List<Metric> metrics = new ArrayList<>();
		for(MetricConfig config : this.metrics){
			metrics.add(config.createMetric());
		}

		StorageManager.getInstance().addStorages(storages);
		MetricManager.setMetrics(metrics);
	}


	private String generateSuiteID() {
		int currentTimeMillisHashCode = Math.abs(Long.valueOf(Instant.now().getEpochSecond()).hashCode());
		return String.valueOf(currentTimeMillisHashCode);
	}
	
}
