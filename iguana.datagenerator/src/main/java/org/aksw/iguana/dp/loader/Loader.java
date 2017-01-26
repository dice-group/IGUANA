/**
 * 
 */
package org.aksw.iguana.dp.loader;

/**
 * 
 * A Class for loading a dataset into a triplestore
 * 
 * @author f.conrads
 *
 */
public interface Loader {

	/**
	 * Will initialize a Loader with a given connectionID 
	 * 
	 * @param connID
	 * @throws Exception
	 */
	public void init(String connID) throws Exception;
	
	/**
	 * Will upload a byte array to the connection
	 * 
	 * @param data
	 * @throws Exception
	 */
	public void upload(byte[] data) throws Exception;

	/**
	 * Will set the datasetID 
	 * 
	 * @param datasetID
	 */
	public void setDatasetID(String datasetID);
	
}
