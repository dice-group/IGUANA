package org.aksw.iguana.rp.metrics;

import org.aksw.iguana.cc.tasks.stresstest.storage.StorageManager;

import java.util.Properties;

/**
 * This is the Interface for all Metrics
 * 
 * @author f.conrads
 *
 */
public interface Metric {

	/**
	 * This method should implement what to do with one result. <br/><br/>
	 * 
	 * For example: No Of Queries Per Hour will get the time query time
	 * add the time to a variable which keeps track of the total time of all executed queries 
	 * and increase the number of executed queries if the query was successfully executed.<br/><br/>
	 *  
	 * Be aware, that in this example, the Metric could be stopped as soon as one hour is reached,
	 * or it could be calculate in the close method.  <br/><br/>
	 * 
	 * Assuming, the totaltime is in minutes (it should be calculated in ms though)
	 * Latter one will result in the following formular:  <br/>
	 * m = 60 * queries / totaltime<br/><br/>
	 * 
	 * The actual keys of the properties will depend on the core.<br/>
	 * The stress test will send different keys than a completeness test.<br/>
	 * Thus not all metrics are available for each test.  <br/>
	 * Hence it should be implemented if the Metric cannot calculate the test results 
	 * that it will just close itself without adding results. 
	 * 
	 *  
	 * @param p
	 */
	public void receiveData(Properties p);
	
	public void setStorageManager(StorageManager sManager);
	
	public StorageManager getStorageManager();
	/**
	 * This method will be called, as soon as the associated Experiment Task is finished. 
	 *
	 * Not all metrics are available for each test.  
	 * Hence it should be implemented if the Metric cannot calculate the test results 
	 * that it will just close itself without adding results.  
	 * The {@link org.aksw.iguana.rp.metrics.MetricManager} will try to close the Metric still, 
	 * thus it should be checked if that was the case.
	 * 
	 */
	public void close();
	
	
	/**
	 * This method should return a short description of what the Metric will calculate
	 * 
	 * For example (No. of Queries Per Hour): "Will sum up all successful executed Queries in one hour." 
	 * 
	 * @return
	 */
	public String getDescription();
	
	/**
	 * This method should return the Metric Name 
	 * 
	 * For example: "Query Mixes Per Hour"
	 * 
	 * @return
	 */
	public String getName();
	
	/**
	 * This method should return an abbreviated version of the Metric name.
	 * 
	 * For example (Query Mixes Per Hour): "QMPH"
	 * @return
	 */
	public String getShortName();
	
	/**
	 * This method will be called by the {@link org.aksw.iguana.rp.experiment.ExperimentManager} to 
	 * provide meta data such as the number of query mixes. 
	 * 
	 * @param metaData
	 */
	public void setMetaData(Properties metaData);
}
