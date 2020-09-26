/**
 * 
 */
package org.aksw.iguana.cc.tasks;

import org.aksw.iguana.cc.config.elements.Connection;

import java.io.IOException;
import java.util.Properties;

/**
 * A simple Task to execute
 * 
 * @author f.conrads
 *
 */
public interface Task {

	/**
	 * Will execute the Task 
	 */
	public void execute();

	/**
	 * Will start the Task (sending the rabbitMQ start flag)
	 */
	public void start();
	
	/**
	 * Will send the results to the result processing.
	 * @param data
	 * @throws IOException
	 */
	void sendResults(Properties data) throws IOException;


	/**
	 * Will close the Task and post process everything (e.g. send the end flag to the rabbit mq queue)
	 */
	void close();

	/**
	 * Will add the Meta data for the start which then can be saved into the triple based storages
	 */
	void addMetaData();


	/**
	 * Will initialize the task
	 * @param ids normally the suiteID, experimentID, taskID
	 * @param dataset the dataset name
	 * @param con the current connection to execute the task against
	 */
    void init(String[] ids, String dataset, Connection con);
}
