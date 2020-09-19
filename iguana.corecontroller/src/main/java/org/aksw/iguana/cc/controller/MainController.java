package org.aksw.iguana.cc.controller;

import java.io.File;
import java.io.IOException;

import org.aksw.iguana.cc.config.ConfigManager;

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
		if(argc.length != 1 && argc.length !=2){
			System.out.println("java -jar iguana.jar [--ignore-schema] suite.yml \n\tsuite.yml - The suite containing the benchmark configuration\n\t--ignore-schema - Will not validate configuration using the internal json schema\n");
			return;
		}

		MainController controller = new MainController();
		String config =argc[0];
		Boolean validate = true;
		if(argc.length==2){
			if(argc[0].equals("--ignore-schema")){
				validate=false;
			}
			config = argc[1];
		}
		controller.start(config, validate);
	}

	public void start(String configFile, Boolean validate) throws IOException{
		ConfigManager cmanager = new ConfigManager();
		File f = new File(configFile);
		if (f.length()!=0) {
			cmanager.receiveData(f, validate);
		} else {
			LOGGER.error("Empty configuration.");

		}

	}
}
