package org.aksw.iguana.cc.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;

import org.aksw.iguana.cc.config.ConfigManager;
import org.aksw.iguana.cc.consumer.impl.DefaultConsumer;
import org.aksw.iguana.commons.config.Config;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.exceptions.IguanaException;
import org.aksw.iguana.commons.rabbit.RabbitMQUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
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
	 * @throws IOException 
	 */
	public static void main(String[] argc) throws IOException{
		if(argc.length==1){
			Config.getInstance(argc[0]);
		}
		MainController controller = new MainController();
		if(argc.length>0) {
			controller.start(argc[0]);
		}
		else {
			controller.start();
		}
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
		//configThread.start();
		try {
			consumer.init(host, COMMON.CONFIG2MC_QUEUE_NAME);
		} catch (IguanaException e) {
			LOGGER.error("Could not initalize and start communicator with Host "+host
					+" and consume queue "+COMMON.CONFIG2MC_QUEUE_NAME, e);
			consumer.close();
		}
	}
	
	public void start(String configFile) throws IOException{		
		String host=Config.getInstance().getString(COMMON.CONSUMER_HOST_KEY);

		ConfigManager cmanager = new ConfigManager();
		PropertiesConfiguration config = new PropertiesConfiguration();
		String configStr = FileUtils.readFileToString(new File(configFile));
		System.out.println(configStr);
		try(StringReader sreader = new StringReader(configStr)){
			config.load(sreader);
		} catch (ConfigurationException e1) {
			LOGGER.error("Could not read configuration. Must ignore it... Sorry :(", e1);

		} 
		if (!config.isEmpty()) {
			System.out.println("test");
			cmanager.receiveData(config);
		} else {

			LOGGER.error("Empty configuration. Must ignore it... Sorry :(");

		}

	}
}
