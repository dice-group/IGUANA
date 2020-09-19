/**
 * 
 */
package org.aksw.iguana.cc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


/**
 * Manages an incoming Configuration and starts the corresponding {@link org.aksw.iguana.cc.config.IguanaConfig} 
 * 
 * @author f.conrads
 *
 */
public class ConfigManager {

	private Logger LOGGER = LoggerFactory.getLogger(getClass());


	/**
	 * Will receive a JSON or YAML configuration and executes the configuration as an Iguana Suite
     * @param configuration
     * @param validate checks if error should be thrown if it validates the configuration given the iguana-schema.json schema
     */
	public void receiveData(File configuration, Boolean validate) throws IOException {

		IguanaConfig newConfig = IguanaConfigFactory.parse(configuration, validate);
		if(newConfig==null){
			return;
		}
		startConfig(newConfig);
	}



	/**
	 * Starts the Config
	 */
	public void startConfig(IguanaConfig config) {
		try {
			config.start();
		} catch (IOException e) {
			LOGGER.error("Could not start config due to an IO Exception", e);
		}

	}



}
