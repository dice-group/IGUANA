/**
 * 
 */
package org.aksw.iguana.rp.controller;


import org.aksw.iguana.rp.experiment.ExperimentManager;
import org.aksw.iguana.rp.metrics.Metric;
import org.aksw.iguana.rp.metrics.MetricManager;
import org.aksw.iguana.rp.storage.Storage;
import org.aksw.iguana.rp.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This is the Main Controller. 
 * It will start the ResultProcessor, initialize the {@link org.aksw.iguana.rp.storage.StorageManager} and the {@link org.aksw.iguana.rp.metrics.MetricManager}
 * 
 * @author f.conrads
 *
 */
public class RPController {

	
	private static final Logger LOGGER = LoggerFactory
			.getLogger(RPController.class);

	
	/**
	 * This will initialize the MainController.
	 */
	public void init(List<Storage> storages, List<Metric> metrics){
		//add storages to StoragesManager
		StorageManager storageManager = StorageManager.getInstance();
		storageManager.addStorages(storages);
		LOGGER.info("Storages : {{}}", storageManager);
		//Add default metrics to MetricsManager
		MetricManager globalMetricsManager = MetricManager.getInstance();
		globalMetricsManager.addMetrics(metrics);
		LOGGER.info("GlobalMetrics : {{}}", globalMetricsManager);
		ExperimentManager emanager = new ExperimentManager(globalMetricsManager, storageManager);

	}
	
}
