package org.aksw.iguana.wc.config;

import java.util.ArrayList;

public class Dataset {
	
	private String name;
	private String datasetGeneratorClassName;
	private ArrayList<String> constructorArgs;
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the datasetGeneratorClassName
	 */
	public String getDatasetGeneratorClassName() {
		return datasetGeneratorClassName;
	}
	/**
	 * @param datasetGeneratorClassName the datasetGeneratorClassName to set
	 */
	public void setDatasetGeneratorClassName(String datasetGeneratorClassName) {
		this.datasetGeneratorClassName = datasetGeneratorClassName;
	}
	/**
	 * @return the constructorArgs
	 */
	public ArrayList<String> getConstructorArgs() {
		return constructorArgs;
	}
	/**
	 * @param constructorArgs the constructorArgs to set
	 */
	public void setConstructorArgs(ArrayList<String> constructorArgs) {
		this.constructorArgs = constructorArgs;
	}
}
