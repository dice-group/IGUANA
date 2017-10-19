/**
 * 
 */
package org.aksw.iguana.dg.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.aksw.iguana.commons.rabbit.RabbitMQUtils;
import org.aksw.iguana.dp.loader.LoaderManager;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

/**
 * This abstract class will implement all the underlying methods 
 * to send the data to the loader Manager in well sized snippets
 * 
 * @author f.conrads
 *
 */
public abstract class AbstractDataGenerator  implements
		DataGenerator {

	protected int maxSize=100;
	protected String datasetID;

	protected Model data = ModelFactory.createDefaultModel();

	private LoaderManager lmanager;
	
	/**
	 * will call the method {@link #generate()} and afterwars will send the inner Model {@link #data}
	 */
	public void generateData() throws Exception {
		generate();
		sendData(data);
	}
	
	/**
	 * This method should generate your data,</br></br>
	 * 
	 * to make use of the underlying abstract class, please add your data to the data Model
	 * or elsewise make use of the sendDataSnippet(byte[]) or sendDataComplete(Model) methods.
	 * If you add your data to the Model data. You do not have to send it. It will be sended
	 * as soon the generate() method stops.</br></br>
	 * 
	 * if your model is too big though, you can send snippets of the dataset
	 * 
	 * you can use the sendData(Model) method for this still. </br>
	 * It will automatically send the model in the accurate size of blocks to the controller.
	 */
	public abstract void generate() throws Exception;

	/**
	 * Will send the byte array to the loader Manager
	 */
	public void sendDataSnippet(byte[] data) throws Exception {
		//sendData to LoaderManager
		lmanager.upload(data, datasetID);
	}

	/**
	 * Will send a {@link org.apache.jena.rdf.model.Model} 
	 */
	public void sendData(Model m) throws Exception {
		int sendedSize=0;
		//split Model into small parts
		StmtIterator sti = m.listStatements();
		while(sti.hasNext()){
			Model send = ModelFactory.createDefaultModel();
			send.setNsPrefixes(m.getNsPrefixMap());
			while(sendedSize < maxSize){
				Statement stmt = sti.next();
				send.add(stmt);
				sendedSize+=1;
			}
			sendDataSnippet(RabbitMQUtils.getData(send));
		}
		
	}

	/**
	 * Will send an Ntriple File in well sized snippets
	 */
	public void sendData(File ntripleFile) throws Exception {
		Model send = ModelFactory.createDefaultModel();
		int sendedSize=0;
		//Read file line by line
		try(BufferedReader reader = new BufferedReader(new FileReader(ntripleFile))){
			String triple="";
			while(null!=(triple=reader.readLine())){
				
				send.read(triple);
				if(sendedSize >= maxSize){
					sendDataSnippet(RabbitMQUtils.getData(send));
					send = ModelFactory.createDefaultModel();
				}
				
			}
			//if send has still data. send it and close the model
			if(!send.isEmpty()){
				sendDataSnippet(RabbitMQUtils.getData(send));
				send.close();
			}
		}catch(IOException e){
			throw e;
		}
	}
	
	/**
	 * Will set the Loader Manager which should handle the upload
	 */
	public void setLoaderManager(LoaderManager lmanager){
		this.lmanager = lmanager;
	}
	
}
