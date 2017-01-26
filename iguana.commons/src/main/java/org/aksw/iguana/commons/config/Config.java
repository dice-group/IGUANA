package org.aksw.iguana.commons.config;

import org.aksw.iguana.commons.constants.COMMON;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Configuration Class for loading a configuration Properties file
 * @author f.conrads
 *
 */
public class Config {

	private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    private static Configuration instance = null;
    
    private static String fileName = COMMON.DEFAULT_IGAUNA_RP_PROPERTIES_FILE_NAME;

    /**
     * Get the default instance
     * @return
     */
    public static synchronized Configuration getInstance(){
    	return getInstance(fileName);
    }
    
    /**
     * Gets the already loaded instance. If there is not loaded an instance yet. 
     * The method will try to load an instance by the provided fileName.
     * 
     * @param fileName
     * @return
     */
    public static synchronized Configuration getInstance(String fileName) {
        if (instance == null) {
            instance = new CompositeConfiguration();
            loadAdditionalProperties(fileName);
        }
        return instance;
    }
    
    /**
     * Loads a Properties Configuration file assoc. with the fileName
     * 
     * @param fileName
     */
    public static synchronized void loadAdditionalProperties(String fileName) {
        try {
            ((CompositeConfiguration) getInstance()).addConfiguration(new PropertiesConfiguration(fileName));
        } catch (ConfigurationException e) {
            LOGGER.error("Could not load Properties from the properties file (\"" + fileName
                    + "\"). This Iguana instance won't work as expected.", e);
        }
    }
    
    /**
     * Gets the current maven version
     * 
     * @return
     */
    public static String getIguanaModuleVersion() {
        return Config.class.getPackage().getImplementationVersion();
    }

    /**
     * Gets the current name of the configuration file to load
     * 
     * @return
     */
	public static String getFileName() {
		return fileName;
	}

	/**
	 * Sets the configuration file to load
	 * 
	 * @param fileName
	 */
	public static void setFileName(String fileName) {
		Config.fileName = fileName;
	}
}
