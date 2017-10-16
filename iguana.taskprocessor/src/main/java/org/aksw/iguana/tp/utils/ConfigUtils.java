package org.aksw.iguana.tp.utils;

import java.util.Iterator;

import org.apache.commons.configuration.Configuration;

public class ConfigUtils {

	/**
	 * @param config
	 * @param suffix
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getObjectWithSuffix(Configuration config, String suffix) {
		Iterator<String> keySet = config.getKeys();
		while(keySet.hasNext()) {
			String key = keySet.next();
			if(key.endsWith(suffix)) {
				return (T) config.getProperty(key);
			}
		}
		return null;
	}
	
	/**
	 * @param config
	 * @param suffix
	 * @return
	 */
	@SuppressWarnings("unchecked")
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
