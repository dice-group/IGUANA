/**
 * 
 */
package org.aksw.iguana.cc.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.config.elements.Task;
import org.aksw.iguana.cc.tasks.TaskFactory;
import org.aksw.iguana.cc.tasks.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Task Controlling, will start communication between core controller and result processor
 * will start consumer for cc and a sender for cc as well as rp.  
 * Will recv a cc message, creates and starts the task,  will send the results to rp and 
 * after finishing the task will send a task finished flag back to cc.
 * 
 * @author f.conrads
 *
 */
public class TaskController {

	private static Map<String, String> shortHandMap = new HashMap<String, String>();

	private static final Logger LOGGER = LoggerFactory
			.getLogger(TaskController.class);

	public void startTask(String[] ids, String dataset, Connection con, Task task) {
		TaskManager tmanager = new TaskManager();
		String className=task.getClassName();
		TaskFactory factory = new TaskFactory();
		tmanager.setTask(factory.create(className, task.getConfiguration()));
		try {
			tmanager.startTask(ids, dataset, con);
		} catch (IOException | TimeoutException e) {
			LOGGER.error("Could not start Task "+className, e);
		}
	}


}
