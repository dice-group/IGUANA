package org.aksw.iguana.cc.worker;

import org.aksw.iguana.cc.tasks.impl.Stresstest;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

/**
 * Interface for the Worker Thread used in the {@link Stresstest}
 * 
 * @author f.conrads
 *
 */
public interface Worker extends Runnable{

	
	/**
	 * This method executes a query and adds the results to the Result Processor for proper result and metric calculations.
	 * Note: Some of the Worker implementations employ background threads to process the result of the query.
	 * Due to this, this method does not return anything and each implementation of this method must also add the
	 * results to Result Processor within this method. This can be done by calling AbstractWorker.addResults(QueryExecutionStats)
	 *  @param query The query which should be executed
	 * @param queryID the ID of the query which should be executed
	 */
	public void executeQuery(String query, String queryID);
	
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

	boolean isTerminated();

	/**
	 * Returns the no of queries in the queryset of the worker
	 * @return
	 */
	long getNoOfQueries();

	/**
	 * Returns if the no of query mixes were already executed
	 * @param noOfQueryMixes
	 * @return
	 */
	boolean hasExecutedNoOfQueryMixes(double noOfQueryMixes);
}
