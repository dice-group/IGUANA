package org.aksw.iguana.cc.query;

import org.aksw.iguana.cc.query.set.QuerySet;
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
	Map<String, QuerySet[]> generate();

	/**
	 * Generates some stats for the queries
	 * @param taskID
	 * @return
	 */
	Model generateTripleStats(String taskID);
	
}
