/**
 * 
 */
package org.aksw.iguana.dg.generator;

import java.io.File;
import java.io.IOException;

import org.aksw.iguana.dp.loader.LoaderManager;
import org.apache.jena.rdf.model.Model;

/**
 * The DataGenerator Interface. </br>
 * Please implement the HobbitDataGenerator and not the direct Interface.</br>
 * The Hobbit DG will work with both Hobbit and the Iguana standalone version
 * 
 * @author f.conrads
 *
 */
public interface DataGenerator {

	/**
	 * Initialization method
	 */
	public void init() throws Exception;
	
	/**
	 * This should generate your data
	 * @return 
	 * @throws Exception 
	 * 
	 */
	public void generate() throws Exception;
	
	/**
	 * send data snippet, 
	 * best to use this if possible.
	 * Use this method in the generate method, to always send some small snippets.
	 * 
	 * @param data
	 * @throws Exception 
	 */
	public void sendDataSnippet(byte[] data) throws Exception;
	
	/**
	 * Only if absolutely must, you can completely send a Model. 
	 * The method should still send in Snippets
	 * 
	 * @param m
	 * @throws Exception 
	 */
	public void sendData(Model m) throws Exception;

	/**
	 * Only if absolutely must, you can completely send a Ntriple File. 
	 * The method should still send in Snippets
	 *
	 * @param ntripleFile
	 * @throws IOException 
	 * @return The datasetID
	 */
	public void sendData(File ntripleFile) throws Exception;

	/**
	 * Should contain the generate method and sending the data to the loader manager
	 * 
	 * @throws Exception
	 */
	public void generateData() throws Exception;

	/**
	 * Will set the Loader Manager which should handle the upload
	 * 
	 * @param lmanager
	 */
	public void setLoaderManager(LoaderManager lmanager);


}
