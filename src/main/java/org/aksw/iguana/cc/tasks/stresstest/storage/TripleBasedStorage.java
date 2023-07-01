/**
 * 
 */
package org.aksw.iguana.cc.tasks.stresstest.storage;

import org.aksw.iguana.commons.constants.COMMON;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * This Storage will save all the metric results as triples
 * 
 * @author f.conrads
 *
 */
public abstract class TripleBasedStorage implements Storage {

	protected String baseUri = COMMON.BASE_URI;
	protected Model metricResults = ModelFactory.createDefaultModel();

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	public void storeResult(Model data){
		metricResults.add(data);
	}
}
