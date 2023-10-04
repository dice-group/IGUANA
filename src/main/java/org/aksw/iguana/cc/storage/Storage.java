package org.aksw.iguana.cc.storage;

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
	 * Depending on the storages format, the storage class will need convert the data into the appropriate format.
	 *
	 * @param data the given result model
	 */
	void storeResult(Model data);

	/**
	 * General purpose method to store data into the storage.
	 * This method will mostly be used by the language processors to store their already formatted data. <br/>
	 * The default implementation will call the {@link #storeResult(Model)} method. This might not be the best solution
	 * for storages, that do not use RDF as their format.
	 *
	 * @param data the data to store
	 */
	default void storeData(Storable data) {
		if (data instanceof Storable.AsRDF) {
			storeResult(((Storable.AsRDF) data).toRDF());
		}
	}
}
