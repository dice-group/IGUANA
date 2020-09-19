package org.aksw.iguana.cc.utils;

import org.apache.commons.configuration.Configuration;

import java.util.Iterator;

/**
 * a few methods to work with Configurations more easily
 * 
 * @author f.conrads
 *
 */
public class ConfigUtils {

	/**
	 * @param config
	 * @param suffix
	 * @return
	 */
	public static String getObjectWithSuffix(Configuration config, String suffix) {
		Iterator<String> keySet = config.getKeys();
		while(keySet.hasNext()) {
			String key = keySet.next();
			if(key.endsWith(suffix)) {
				return config.getProperty(key).toString();
			}
		}
		return null;
	}
	
	/**
	 * @param config
	 * @param suffix
	 * @return
	 */
	public static String[] getStringArrayWithSuffix(Configuration config, String suffix) {
		Iterator<String> keySet = config.getKeys();
		while(keySet.hasNext()) {
			String key = keySet.next();
			if(key.endsWith(suffix)) {
				return config.getStringArray(key);
			}
		}
		return null;
	}


}
