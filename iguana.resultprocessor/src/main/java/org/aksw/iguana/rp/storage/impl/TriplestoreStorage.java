/**
 * 
 */
package org.aksw.iguana.rp.storage.impl;

import org.aksw.iguana.rp.config.CONSTANTS;
import org.aksw.iguana.rp.storage.TripleBasedStorage;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

import java.io.StringWriter;
import java.util.Properties;


/**
 * This Storage will save all the metric results into a specified triple store
 * 
 * @author f.conrads
 *
 */
public class TriplestoreStorage extends TripleBasedStorage {
	
	private UpdateRequest blockRequest = UpdateFactory.create();

	
	private String updateEndpoint;
	private String endpoint;
	private String user;
	private String pwd;

	
	public TriplestoreStorage(String endpoint, String updateEndpoint, String user, String pwd, String baseUri){
		this.endpoint=endpoint;
		this.updateEndpoint=updateEndpoint;
		this.user=user;
		this.pwd=pwd;
		if(baseUri!=null && !baseUri.isEmpty()){
			this.baseUri=baseUri;
		}
	}
	
	public TriplestoreStorage(String endpoint, String updateEndpoint, String baseUri){
		this.endpoint=endpoint;
		this.updateEndpoint=updateEndpoint;
		if(baseUri!=null && !baseUri.isEmpty()){
			this.baseUri=baseUri;
		}
	}
	
	public TriplestoreStorage(String endpoint, String updateEndpoint){
		this.endpoint=endpoint;
		this.updateEndpoint=updateEndpoint;
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.iguana.rp.storage.Storage#commit()
	 */
	@Override
	public void commit() {
		if (metricResults.size() == 0)
			return;

		StringWriter results = new StringWriter();
		RDFDataMgr.write(results, metricResults, Lang.NT);
		String update = "INSERT DATA {" + results.toString() + "}";
		//Create Update Request from block
		blockRequest.add(update);

		//submit Block to Triple Store
		UpdateProcessor processor = UpdateExecutionFactory
				.createRemote(blockRequest, updateEndpoint, createHttpClient());
		processor.execute();
		blockRequest = new UpdateRequest();
	}

	/* (non-Javadoc)
	 * @see org.aksw.iguana.rp.storage.Storage#getStorageInfo()
	 */
	@Override
	public Properties getStorageInfo() {
		//Only sets the endpoint to use for result viewing
		Properties ret = new Properties();
		ret.setProperty(CONSTANTS.STORAGE_ENDPOINT, endpoint);
		return ret;
	}

	private HttpClient createHttpClient(){
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		if(user !=null && pwd !=null){
			Credentials credentials = new UsernamePasswordCredentials(user, pwd);
			credsProvider.setCredentials(AuthScope.ANY, credentials);
		}
		HttpClient httpclient = HttpClients.custom()
		    .setDefaultCredentialsProvider(credsProvider)
		    .build();
		return httpclient;
	}

	@Override
	public String toString(){
		return this.getClass().getSimpleName();
	}
}
