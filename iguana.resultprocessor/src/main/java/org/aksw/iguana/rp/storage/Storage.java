package org.aksw.iguana.rp.storage;

import java.util.Properties;

import org.aksw.iguana.rp.data.Triple;

/**
 * Interface for the Result Storages
 * 
 * @author f.conrads
 *
 */
public interface Storage {
	
	/**
	 * Add actual result data from metrics
	 * @param meta the meta data (metric, experimentID,...)
	 * @param data the actual data to add
	 */
	public void addData(Properties meta, Triple[] data);
	
	/**
	 * Add meta data from the experiment task
	 * for example: Query ID and Query text, ExperimentID, WorkerID,...
	 * 
	 * @param p
	 */
	public void addMetaData(Properties p);
	
	/**
	 * Commit a DataBlock to the Storage. 
	 * Keep in mind, that all received Data is from the metrics and should be saved as it is. 
	 * 
	 */
	public void commit();
	
	/**
	 * Should return the information of the Storage. 
	 * For Example: SPARQL endpoint, Folder directory,...
	 * @return
	 */
	public Properties getStorageInfo();

	public void endTask(String taskID);
	
}
