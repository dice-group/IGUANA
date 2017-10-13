package org.aksw.iguana.wc.config;

import java.util.ArrayList;

/**
 * The web dataset config
 * 
 * @author f.conrads
 *
 */
public class Dataset {
	
	private String name;
	private String datasetGeneratorClassName;
	private ArrayList<String> constructorArgs;
	
	/**
	 * Gets the name of the dataset
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the  name of the dataset
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Gets the class name of the data generator to use
	 * 
	 * @return the datasetGeneratorClassName
	 */
	public String getDatasetGeneratorClassName() {
		return datasetGeneratorClassName;
	}
	
	/**
	 * Sets the class name of the data generator to use
	 * 
	 * @param datasetGeneratorClassName the datasetGeneratorClassName to set
	 */
	public void setDatasetGeneratorClassName(String datasetGeneratorClassName) {
		this.datasetGeneratorClassName = datasetGeneratorClassName;
	}
	
	/**
	 * Gets the constructor Arguments of the data generator
	 * 
	 * @return the constructorArgs
	 */
	public ArrayList<String> getConstructorArgs() {
		return constructorArgs;
	}
	
	/**
	 * Sets the constructor Arguments of the data generator
	 * 
	 * @param constructorArgs the constructorArgs to set
	 */
	public void setConstructorArgs(ArrayList<String> constructorArgs) {
		this.constructorArgs = constructorArgs;
	}
}
