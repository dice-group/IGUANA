package org.aksw.iguana.rp.storage;

import java.util.Properties;

import org.apache.jena.rdf.model.Model;

/**
 * Interface for the Result Storages
 * 
 * @author f.conrads
 *
 */
public interface Storage {
	


	/**
	 * Add Triples as they are
	 * @param data
	 */
	public void addData(Model data);


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
	


	public void endTask(String taskID);


}
