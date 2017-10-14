package org.aksw.iguana.tp.consumer.impl;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.commons.communicator.Communicator;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.consumer.AbstractConsumer;
import org.aksw.iguana.commons.rabbit.RabbitMQUtils;
import org.aksw.iguana.tp.tasks.TaskFactory;
import org.aksw.iguana.tp.tasks.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default consumer for the task processing
 * @author f.conrads
 *
 */
public class DefaultConsumer extends AbstractConsumer{

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DefaultConsumer.class);
	
	private TaskManager tmanager;

	private Communicator parent;

	/**
	 * Will set a TaskManager to add and start tasks with as soon as a message arrives
	 * @param tmanager
	 */
	public DefaultConsumer(TaskManager tmanager) {
		this.tmanager= tmanager;
	}

	/**
	 * Sets the parent communicator to send back flags 
	 * @param parent
	 */
	public void setParent(Communicator parent){
		this.parent = parent;
	}
	
	@Override
	public void consume(byte[] data) {
		Properties p = RabbitMQUtils.getObject(data);
		consume(p);
	}

	/**
	 * Consumes a Properties object instead of a byte object
	 *  
	 * @param p
	 */
	public void consume(Properties p) {
		String className=p.getProperty(COMMON.CLASS_NAME);
		Object[] constructorArgs=(Object[]) p.get(COMMON.CONSTRUCTOR_ARGS);
		Class<?>[] constructorClasses=null;
		if(p.containsKey(COMMON.CONSTRUCTOR_ARGS_CLASSES)){
			constructorClasses = (Class[]) p.get(COMMON.CONSTRUCTOR_ARGS_CLASSES);
		}
		
			
		TaskFactory factory = new TaskFactory();
		tmanager.setTask(factory.create(className, constructorArgs, constructorClasses));
		try {
			tmanager.startTask();
			parent.send(RabbitMQUtils.getData(COMMON.TASK_FINISHED_MESSAGE));
		} catch (IOException | TimeoutException e) {
			LOGGER.error("Could not start Task "+className, e);
			parent.send(RabbitMQUtils.getData(COMMON.TASK_FINISHED_MESSAGE));
		}
	}
	
	
}
