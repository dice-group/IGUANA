package org.aksw.iguana.tp.query;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.Worker;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.SPARQLWorker;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.UPDATEWorker;

public abstract class AbstractWorkerQueryHandler implements QueryHandler{

	/**
	 * Will contain the path of the worker specified query files to 
	 * the Files where the final querys will be saved
	 */
	private Map<String, File[]> mapping = new HashMap<String, File[]>();
	private HashSet<String> sparqlKeys = new HashSet<String>();
	private HashSet<String> updateKeys = new HashSet<String>();
	private Collection<Worker> workers; 
	
	public AbstractWorkerQueryHandler(Collection<Worker> workers) {
		this.workers = workers;
		for(Worker worker : workers) {
			if(worker instanceof SPARQLWorker) {
				sparqlKeys.add(((SPARQLWorker)worker).getQueriesFileName());
			}
			else if(worker instanceof UPDATEWorker) {
				updateKeys.add(((UPDATEWorker)worker).getQueriesFileName());
			}
		}
	}
	
	@Override
	public void generateQueries() {
		for(String sparqlKey : sparqlKeys) {
			mapping.put(sparqlKey, generateSPARQL(sparqlKey));
		}
		for(String updateKey : updateKeys) {
			mapping.put(updateKey, generateUPDATE(updateKey));
		}
		for(Worker worker : workers) {
			if(worker instanceof AbstractWorker) {
				((AbstractWorker)worker).setQueriesList(
						mapping.get(((AbstractWorker)worker).getQueriesFileName()));
			}
		}
	}
	
	protected abstract File[] generateSPARQL(String queryFileName) ;
	
	protected abstract File[] generateUPDATE(String updatePath) ;
	
}
