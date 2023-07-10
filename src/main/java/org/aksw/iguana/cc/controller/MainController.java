package org.aksw.iguana.cc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.text.MessageFormat.format;

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
		LOGGER.info("Stopping Iguana");
		//System.exit(0);
	}

	/**
	 * Starts a configuration using the config file an states if Iguana should validate it using a json-schema
	 *
	 * @param configFile the Iguana config file
	 * @param validate should the config file be validated using a json-schema
	 * @throws IOException
	 */
	public void start(String configFile, Boolean validate) throws IOException{
		var f = Path.of(configFile);
		if (Files.isReadable(f)) {
		} else {
			LOGGER.error(format("Configuration file does not exist or is not readable: {0}", f.toString()));

		}

	}
}
