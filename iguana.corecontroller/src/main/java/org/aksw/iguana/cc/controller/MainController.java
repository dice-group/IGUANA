package org.aksw.iguana.cc.controller;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.aksw.iguana.cc.config.ConfigManager;
import org.aksw.iguana.commons.config.Config;
import org.aksw.iguana.commons.constants.COMMON;
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
		if(argc.length != 1){
			LOGGER.error("Please provide the path to an IGUANA suite file as single argument.");
		}
		Config.getInstance(argc[0]);
		MainController controller = new MainController();
		controller.start(argc[0]);
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
