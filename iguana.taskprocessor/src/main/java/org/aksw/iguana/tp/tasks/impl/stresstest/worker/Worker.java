package org.aksw.iguana.tp.tasks.impl.stresstest.worker;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

/**
 * Interface for the Worker Thread used in the {@link org.aksw.iguana.tp.tasks.impl.stresstest.Stresstest}
 * 
 * @author f.conrads
 *
 */
public interface Worker extends Runnable{

	
	/**
	 * This should return the time in ms the query took to be executed and checked for results
	 * If an error occured -1 should be returned
	 * 
	 * @param query The query which should be executed
	 * @param queryID the ID of the query which should be executed
	 * @return the time/ms the query took to executed, -1 if error happend
	 */
	public Long getTimeForQueryMs(String query, String queryID);
	
	/**
	 * This method saves the next query in the queryStr StringBuilder and
	 * the query id in the queryID.
	 * 
	 * @param queryStr The query should be stored in here!
	 * @param queryID The queryID should be stored in here!
	 * @throws IOException 
	 */
	public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException;
	
	/**
	 * This will initialize the worker.
	 * It will be called before starting the worker
	 * 
	 * @param p The properties for the worker.
	 */
	public void init(Properties p);

	/**
	 * This should stop the next sending process.
	 * If an execution started before this method was called, but answered after, it should not be counted!
	 */
	public void stopSending();

	/**
	 * This will simulate the Time in ms to wait before testing the next query.
	 * It can be used to simulate network delay.
	 */
	public void waitTimeMs();

	
	/**
	 * This will return the amount of executed queries so far
	 *
	 * @return no. of executed queries
	 */
	public long getExecutedQueries();
	
	/**
	 * Get and remove all internal stored results of finished queries
	 * 
	 * @return list of Properties to send to RabbitMQ
	 */
	public Collection<Properties> popQueryResults();

	/**
	 * @param args
	 */
	void init(String[] args);
}
