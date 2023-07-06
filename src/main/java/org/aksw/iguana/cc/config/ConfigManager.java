/**
 * 
 */
package org.aksw.iguana.cc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;


/**
 * Manages an incoming Configuration and starts the corresponding {@link org.aksw.iguana.cc.config.IguanaConfig} 
 * 
 * @author f.conrads
 *
 */
public class ConfigManager {

	private final Logger LOGGER = LoggerFactory.getLogger(getClass());


	/**
	 * Will receive a JSON or YAML configuration and executes the configuration as an Iguana Suite
     * @param config
     * @param validate checks if error should be thrown if it validates the configuration given the iguana-schema.json schema
     */
	public void receiveData(Path config, Boolean validate) throws IOException {

		IguanaConfig newConfig = IguanaConfigFactory.parse(config, validate);
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
