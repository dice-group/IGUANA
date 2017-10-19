/**
 * 
 */
package org.aksw.iguana.dp.loader;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager for loading a dataset to a mapped connection
 * 
 * @author f.conrads
 *
 */
public class LoaderManager {

	private Map<String, Loader> idToLoader = new HashMap<String, Loader>();



	/**
	 * Will add one Loader and assoc. it to the connectionID
	 * 
	 * @param connectionID
	 * @param loader
	 */
	public void addLoaderForID(String connectionID, Loader loader){
		idToLoader.put(connectionID, loader);
	}
	
	/**
	 * will initialize all Laoder Objects with their given connectionIDs
	 * 
	 * @throws Exception
	 */
	public void initAll() throws Exception{
		for(String connectionID : idToLoader.keySet()){
			Loader l  = idToLoader.get(connectionID);
			l.init(connectionID);
		}
	}
	
	/**
	 * Will initialize the Loader assoc. with connectionID 
	 * 
	 * @param connectionID
	 * @throws Exception
	 */
	public void init(String connectionID) throws Exception{
		idToLoader.get(connectionID).init(connectionID);
	}
	
	/**
	 * Will upload data the Loader assoc with the connectionID.
	 * Will not set datasetID!
	 * 
	 * @param connectionID
	 * @param data
	 * @throws Exception
	 */
	@Deprecated
	public void upload(String connectionID, byte[] data) throws Exception{
		idToLoader.get(connectionID).upload(data);
	}
	
	/**
	 * Will upload a data byte array, assoc with the datasetID with all inner Loader 
	 * 
	 * @param data
	 * @param datasetID
	 * @throws Exception
	 */
	public void upload(byte[] data, String datasetID) throws Exception{
		for(Loader l : idToLoader.values()){
			l.setDatasetID(datasetID);
			l.upload(data);
		}
	}
}
