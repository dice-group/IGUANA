package org.aksw.iguana.tp.query;

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
	 */
	public void generateQueries();

	public String generateTripleStats(String taskID, String resource, String property);
	
}
