package org.aksw.iguana.tp.query;

import org.apache.jena.rdf.model.Model;

import java.io.File;
import java.util.Map;

/**
 * The QueryHandler interface
 * <br/> 
 * The QueryHandler can be used to generate queries in the Tasks. 
 * 
 * @author f.conrads
 *
 */
public interface QueryHandler {

	/**
	 * This will generate the queries.
	 * @return
	 */
	public Map<String, File[]> generateQueries();

	public Model generateTripleStats(String taskID, String resource, String property);
	
}
