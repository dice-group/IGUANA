package org.aksw.iguana.cc.query;

import org.aksw.iguana.cc.query.set.QuerySet;
import org.aksw.iguana.cc.worker.AbstractWorker;
import org.aksw.iguana.cc.worker.Worker;
import org.aksw.iguana.cc.worker.impl.UPDATEWorker;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * 
 * An abstract class to use if the QueryHandler should work with Workers. (e.g. in the Stresstest Task)
 * 
 * @author f.conrads
 *
 */
public abstract class AbstractWorkerQueryHandler implements QueryHandler{

	/**
	 * Will contain the path of the worker specified query files to 
	 * the Files where the final querys will be saved
	 */
	private Map<String, QuerySet[]> mapping = new HashMap<String, QuerySet[]>();
	private HashSet<String> sparqlKeys = new HashSet<String>();
	private HashSet<String> updateKeys = new HashSet<String>();
	private Collection<Worker> workers; 
	
	/**
	 * 
	 * @param workers
	 */
	public AbstractWorkerQueryHandler(Collection<Worker> workers) {
		this.workers = workers;
		for(Worker worker : workers) {
			if(worker instanceof UPDATEWorker) {
				updateKeys.add(((UPDATEWorker)worker).getQueriesFileName());
			} else {
				sparqlKeys.add(((AbstractWorker)worker).getQueriesFileName());
			}
		}
	}
	
	@Override
	public Map<String, QuerySet[]> generate() {
		for(String sparqlKey : sparqlKeys) {
			mapping.put(sparqlKey, generateQueries(sparqlKey));
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
		return mapping;
	}
	
	/**
	 * This method will generate SPARQL Queries given a file with queries.
	 * 
	 * @param queryFileName The queries file
	 * @return for each query in the file, a File representing the query
	 */
	protected abstract QuerySet[] generateQueries(String queryFileName) ;

	/**
	 * This method will generate UPDATE Queries given a folder with files in which updates are stated.
	 * 
	 * @param updatePath The path to the updates
	 * @return for each update, a File representing it.
	 */
	protected abstract QuerySet[] generateUPDATE(String updatePath) ;
	
}
