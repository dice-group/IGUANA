package org.aksw.iguana.dp.loader;

/**
 * Loader Class with simply implemented setting the connectionID and datasetID
 * 
 * @author f.conrads
 *
 */
public abstract class AbstractLoader implements Loader{

	protected String connID;
	protected String datasetID;
	
	public void init(String connID){
		this.connID = connID;
	}
	
	public void setDatasetID(String datasetID){
		this.datasetID = datasetID;
	}

}
