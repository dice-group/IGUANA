package org.aksw.iguana.cc.controller;

import org.aksw.iguana.cc.config.ConfigManager;
import org.aksw.iguana.cc.consumer.impl.DefaultConsumer;
import org.aksw.iguana.commons.config.Config;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.exceptions.IguanaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main controller for the core. 
 * Will execute the Config Manager and the consuming for configurations.
 * 
 * @author f.conrads
 *
 */
public class MainController {

	
	private static final Logger LOGGER = LoggerFactory
			.getLogger(MainController.class);
	
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
		MainController controller = new MainController();
		controller.start();
	}
	
	/**
	 * Will start the controlling, receiving of task properties, 
	 * sending the {@link COMMON.TASK_FINISHED_MESSAGE} to the main controller 
	 */
	public void start(){		
		String host=Config.getInstance().getString(COMMON.CONSUMER_HOST_KEY);

		ConfigManager cmanager = new ConfigManager();
		DefaultConsumer consumer = new DefaultConsumer(cmanager);
		Thread configThread = new Thread(cmanager);
		configThread.start();
		try {
			consumer.init(host, COMMON.CONFIG2MC_QUEUE_NAME);
		} catch (IguanaException e) {
			LOGGER.error("Could not initalize and start communicator with Host "+host
					+" and consume queue "+COMMON.CONFIG2MC_QUEUE_NAME, e);
			consumer.close();
		}
	}
}
