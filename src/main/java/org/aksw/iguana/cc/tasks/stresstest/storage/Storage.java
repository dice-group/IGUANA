package org.aksw.iguana.cc.tasks.stresstest.storage;

import org.apache.jena.rdf.model.Model;

import java.util.Properties;

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


	/**
	 * Will tell the storage that the task with taskID ended
	 * @param taskID
	 */
	public void endTask(String taskID);


	public default void close(){
	}

}
