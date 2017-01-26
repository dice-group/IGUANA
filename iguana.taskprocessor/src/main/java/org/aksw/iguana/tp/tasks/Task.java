/**
 * 
 */
package org.aksw.iguana.tp.tasks;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

/**
 * A simple Task to execute
 * 
 * @author f.conrads
 *
 */
public interface Task {

	/**
	 * Will start the Task 
	 */
	public void start();

	/**
	 * Will send the results to the result processing.
	 * @param data
	 * @throws IOException
	 */
	void sendResults(Properties data) throws IOException;

	/**
	 * Will initialize the Task with a rabbitMQ host and queue to send the results to.
	 * 
	 * @param host
	 * @param queueName
	 * @throws IOException
	 * @throws TimeoutException
	 */
	void init(String host, String queueName) throws IOException, TimeoutException;
}
