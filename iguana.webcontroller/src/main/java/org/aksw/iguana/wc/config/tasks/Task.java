package org.aksw.iguana.wc.config.tasks;

import org.apache.commons.configuration.Configuration;

/**
 * The basic Task config
 * 
 * @author f.conrads
 *
 */
public interface Task {
	
	/**
	 * Get the class name of the task
	 * @return
	 */
	public String getClassName();
	
	/**
	 * Sets the class name of the task
	 * 
	 * @param className
	 */
	public void setClassName(String className);

	/**
	 * gets the constructor arguments (each argument should be either String, String[], or String[][])
	 * @return
	 */
	public Object[] getConstructorArgs();

	public Configuration getSubConfiguration(String taskID);
	
}
