/**
 * 
 */
package org.aksw.iguana.cc.tasks;

import org.aksw.iguana.cc.config.elements.ConnectionConfig;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Will manage the Tasks
 * 
 * @author f.conrads
 *
 */
public class TaskManager {

	private Task task;

	/**
	 * Will simply set the Task to execute
	 * @param task
	 */
	public void setTask(Task task){
		this.task = task;
	}

	/**
	 * Will initialize and start the Task
	 * 
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public void startTask(String[] ids, String dataset, ConnectionConfig con, String taskName) throws IOException, TimeoutException{
		this.task.init(ids, dataset, con, taskName);
		this.task.addMetaData();
		this.task.start();
		this.task.execute();
		this.task.close();
	}


	
}
