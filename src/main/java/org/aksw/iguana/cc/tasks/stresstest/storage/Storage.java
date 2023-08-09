package org.aksw.iguana.cc.tasks.stresstest.storage;

import org.apache.jena.rdf.model.Model;

/**
 * Interface for the Result Storages
 * 
 * @author f.conrads
 *
 */
public interface Storage {

	/**
	 * Stores the task result into the storage. This method will be executed after a task has finished.
	 *
	 * @param data the given result model
	 */
	void storeResult(Model data);
}
