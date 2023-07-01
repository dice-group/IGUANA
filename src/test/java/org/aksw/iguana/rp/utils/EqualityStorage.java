/**
 * 
 */
package org.aksw.iguana.rp.utils;

import org.aksw.iguana.cc.tasks.stresstest.storage.Storage;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * Class to help the Unit Metric Tests. <br/>
 * 
 * Will be initialized with an Array of Triple[].
 * It will be checked if the first received Data is equal to the first Array Object
 * the second recv Data will be checked against the second Object and so on.
 * 
 * @author f.conrads
 *
 */
public class EqualityStorage implements Storage{

	private Model expectedModel;
	private Model actualModel = ModelFactory.createDefaultModel();



	public EqualityStorage( Model expectedModel) {
		this.expectedModel = expectedModel;
	}



	@Override
	public void storeResult(Model data) {
		this.actualModel.add(data);
	}

	public Model getExpectedModel(){
		return this.expectedModel;
	}

	public Model getActualModel(){
		return this.actualModel;
	}
}
