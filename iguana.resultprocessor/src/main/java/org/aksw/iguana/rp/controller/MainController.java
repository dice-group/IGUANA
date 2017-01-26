/**
 * 
 */
package org.aksw.iguana.rp.controller;

import org.aksw.iguana.commons.config.Config;
import org.aksw.iguana.rp.config.CONSTANTS;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.consumer.IConsumer;
import org.aksw.iguana.rp.consumer.impl.DefaultConsumer;
import org.aksw.iguana.commons.exceptions.IguanaException;
import org.aksw.iguana.rp.experiment.ExperimentManager;
import org.aksw.iguana.rp.metrics.MetricFactory;
import org.aksw.iguana.rp.metrics.MetricManager;
import org.aksw.iguana.rp.storage.StorageFactory;
import org.aksw.iguana.rp.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the Main Controller. 
 * It will start the ResultProcessor, the Consumer and managing the metrics as well as Storages
 * 
 * @author f.conrads
 *
 */
public class MainController {

	
	private static final Logger LOGGER = LoggerFactory
			.getLogger(MainController.class);
	private MetricManager globalMetricsManager;
	private StorageManager storageManager;
	
	public static void main(String[] argc){
		MainController controller = new MainController();
		if(argc.length>0){
			Config.setFileName(argc[0]);
		}
		controller.init();
	}
	
	/**
	 * This will initialize the MainController. 
	 * It will start the {@link org.aksw.iguana.rp.consumer.impl.DefaultConsumer}, add the {@link org.aksw.iguana.rp.metricsMetric}s defined in the config file
	 * further on it will add the  {@link org.aksw.iguana.rp.storages.Śtorage}s defined in the config file.
	 * 
	 */
	public void init(){
		
		//get storages from config
		String[] storageNames = Config.getInstance().getStringArray(CONSTANTS.STORAGES_KEY);
		//add storages to StoragesManager
		storageManager = new StorageManager();
		for(String s : storageNames){
			storageManager.addStorage(StorageFactory.createStorage(s));
		}
		LOGGER.info("Storages : {{}}", storageManager);
		//Get metrics from config
		String[] metrics = Config.getInstance().getStringArray(CONSTANTS.METRICS_KEY);
		//Add default metrics to MetricsManager
		globalMetricsManager = new MetricManager();
		for(String m : metrics){
			globalMetricsManager.addMetric(MetricFactory.createMetric(m, storageManager));
		}
		LOGGER.info("GlobalMetrics : {{}}", globalMetricsManager);
		ExperimentManager emanager = new ExperimentManager(globalMetricsManager, storageManager);
		
		//start DefaultConsumer
		IConsumer consumer = new DefaultConsumer(emanager);
		try {
			LOGGER.info("Starting {}", consumer.getClass().getName());
			//This will loop until user decides to exit.
			String host = Config.getInstance().getString(COMMON.CONSUMER_HOST_KEY);
			String queueName = COMMON.CORE2RP_QUEUE_NAME;
			consumer.init(host, queueName); 
		} catch (IguanaException e) {
			LOGGER.error("Will terminate MainController...", e);
			consumer.close();
			return;
		}
	}

}
