/**
 * 
 */
package org.aksw.iguana.tp.tasks;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.configuration.Configuration;

/**
 * Will manage the Tasks
 * 
 * @author f.conrads
 *
 */
public class TaskManager {

	private Task task;
	private String host;
	private String queueName;

	/**
	 * Create a TaskManager with a rabbitMQ host and queue to send results to
	 * 
	 * @param host
	 * @param queueName
	 */
	public TaskManager(String host, String queueName){
		this.host=host;
		this.queueName=queueName;
	}
	
	/**
	 * Will simply set the Task to execute
	 * @param task
	 */
	public void setTask(Task task){
		this.task = task;
	}

	/**
	 * Will initialize the Task with the provided rabbitMQ host and queue name. 
	 * Then will start the Task
	 * 
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public void startTask() throws IOException, TimeoutException{
		this.task.init(host, queueName);
		this.task.start();
		this.task.execute();
		this.task.close();
	}

	/**
	 * Set the actual task configuration to the current task
	 * 
	 * @param taskConfig
	 */
	public void setTaskConfiguration(Configuration taskConfig) {
		this.task.setConfiguration(taskConfig);
	}
	
}
