/**
 * 
 */
package org.aksw.iguana.tp.controller;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.commons.communicator.Communicator;
import org.aksw.iguana.commons.config.Config;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.exceptions.IguanaException;
import org.aksw.iguana.commons.sender.ISender;
import org.aksw.iguana.commons.sender.impl.DefaultSender;
import org.aksw.iguana.tp.consumer.impl.DefaultConsumer;
import org.aksw.iguana.tp.tasks.TaskFactory;
import org.aksw.iguana.tp.tasks.TaskManager;
import org.apache.commons.configuration.Configuration;
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

	private static final Logger LOGGER = LoggerFactory
			.getLogger(TaskController.class);
	
	/**
	 * main method for standalone controlling.
	 * If the TaskController should run standalone instead of in the core itself
	 * 
	 * @param argc
	 */
	public static void main(String[] argc){
		if(argc.length==1){
			Config.getInstance(argc[0]);
		}
		TaskController controller = new TaskController();
		controller.start();
	}
	
	/**
	 * Will start the controlling, receiving of task properties, 
	 * sending the {@link COMMON.TASK_FINISHED_MESSAGE} to the main controller 
	 */
	public void start(){		
		String host=Config.getInstance().getString(COMMON.CONSUMER_HOST_KEY);

		TaskManager tmanager = new TaskManager(host, COMMON.CORE2RP_QUEUE_NAME);
		DefaultConsumer consumer = new DefaultConsumer(tmanager);
		ISender sender = new DefaultSender();
		
		Communicator communicator = new Communicator(consumer, sender);
		consumer.setParent(communicator);
		try {
			communicator.init(host, COMMON.MC2TP_QUEUE_NAME, COMMON.TP2MC_QUEUE_NAME);
		} catch (IguanaException e) {
			LOGGER.error("Could not initalize and start communicator with Host "+host
					+" consume queue "+COMMON.MC2TP_QUEUE_NAME+" and sender queue"+COMMON.TP2MC_QUEUE_NAME, e);
			communicator.close();
		}
	}
	
	/**
	 * start single task set in properties without RabbitMQ
	 * 
	 * @param p Task Properties
 	 */
	public void startTask(Properties p) {
		String host=Config.getInstance().getString(COMMON.CONSUMER_HOST_KEY);
		System.out.println("start task");
		TaskManager tmanager = new TaskManager(host, COMMON.CORE2RP_QUEUE_NAME);
		String className=p.getProperty(COMMON.CLASS_NAME);
		Object[] constructorArgs= (Object[]) p.get(COMMON.CONSTRUCTOR_ARGS);
		Class<?>[] constructorClasses=null;
		if(p.containsKey(COMMON.CONSTRUCTOR_ARGS_CLASSES)){
			constructorClasses = (Class<?>[]) p.get(COMMON.CONSTRUCTOR_ARGS_CLASSES);
		}
		Configuration taskConfig = (Configuration) p.get("taskConfig");
			
		TaskFactory factory = new TaskFactory();
		tmanager.setTask(factory.create(className, constructorArgs, constructorClasses));
		try {
			tmanager.setTaskConfiguration(taskConfig);
			tmanager.startTask();
		} catch (IOException | TimeoutException e) {
			LOGGER.error("Could not start Task "+className, e);
		}
	}
}
