package org.aksw.iguana.commons.config;

import java.io.StringWriter;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * A helper Class for the appache commons Configuration
 * 
 * @author f.conrads
 *
 */
public class ConfigurationUtils {

	
	/**
	 * Converts config to a representive String.
	 * 
	 * If config is not a PropertiesConfiguration it will be converted to it
	 * 
	 * @param config
	 * @return
	 * @throws ConfigurationException
	 */
	public static String convertConfiguration(Configuration config) throws ConfigurationException {
		if(config instanceof PropertiesConfiguration) {
			return convertConfiguration((PropertiesConfiguration)config);
		}
		PropertiesConfiguration config2 = new PropertiesConfiguration();
		return convertConfiguration(config2);		
	}
	
	
	private static String convertConfiguration(PropertiesConfiguration config) throws ConfigurationException {
		StringWriter swriter = new StringWriter();
		config.save(swriter);
		return swriter.toString();
	}
	
	/**
	 * Creates a Properties Configuration of the file with the given name and converts it to a representive string
	 * 
	 * @param fileName
	 * @return
	 * @throws ConfigurationException
	 */
	public static String convertConfiguration(String fileName) throws ConfigurationException {
		PropertiesConfiguration config = new PropertiesConfiguration(fileName);
		return convertConfiguration(config);
	}
}
