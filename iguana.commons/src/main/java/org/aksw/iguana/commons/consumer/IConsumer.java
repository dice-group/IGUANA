package org.aksw.iguana.commons.consumer;

import org.aksw.iguana.commons.exceptions.IguanaException;

/**
 * Consumer class for consuming Core Properties and setting it to the ExperimentManager
 * 
 * @author f.conrads
 *
 */
public interface IConsumer {

	/**
	 * Initialize the Consumer with a rabbitMQ Host and a rabbitMQ queue name 
	 * 
	 * @param host
	 * @param queueName
	 * @throws IguanaException
	 */
	public void init(String host, String queueName) throws IguanaException;

	/**
	 * Closes the Consumer Object
	 */
	public void close();
	
}
