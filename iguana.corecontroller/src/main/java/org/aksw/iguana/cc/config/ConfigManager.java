/**
 * 
 */
package org.aksw.iguana.cc.config;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.configuration.Configuration;

/**
 * Manages an incoming Configuration and starts the corresponding {@link org.aksw.iguana.cc.config.IguanaConfig} 
 * 
 * @author f.conrads
 *
 */
public class ConfigManager implements Runnable {

	private List<IguanaConfig> configs = new LinkedList<IguanaConfig>();
	
	/**
	 * Will receive a {@link org.apache.commons.configuration.Configuration} and add a new IguanConfig to the Queue
	 * @param configuration
	 */
	public void receiveData(Configuration configuration) {
		IguanaConfig newConfig = new IguanaConfig();
		newConfig.setConfig(configuration);
		configs.add(newConfig);
		startConfig();
	}
	
	/**
	 * Start the next Config
	 */
	public void startConfig() {
		if(!configs.isEmpty()) {
			//pop the earliest config 
			IguanaConfig config = configs.remove(0);
			//start the config
			config.start();
		}
	}

	/**
	 * Endless loop.
	 * Will execute All available {@link org.aksw.iguana.cc.config.IguanaConfig} after each other. 
	 */
	public void startConfigs() {
		//FIXME
		while(true) {
			startConfig();
		}
	}
	
	@Override
	public void run() {
		startConfigs();
	}

}
