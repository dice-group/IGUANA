package org.aksw.iguana.cc.tasks.stresstest.storage.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.cc.config.elements.StorageConfig;
import org.aksw.iguana.rp.storage.TripleBasedStorage;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

import java.io.StringWriter;


/**
 * This Storage will save all the metric results into a specified triple store
 *
 * @author f.conrads
 *
 */
public class TriplestoreStorage extends TripleBasedStorage {

	public record Config(@JsonProperty(required = true) String endpoint,
						 String user,
						 String password,
						 String baseUri) implements StorageConfig {
	}

	private UpdateRequest blockRequest = UpdateFactory.create();
	private final String endpoint;
	private final String user;
	private final String password;

	public TriplestoreStorage(Config config) {
		endpoint = config.endpoint();
		user = config.user();
		password = config.password();
		if (baseUri != null && !baseUri.isEmpty()) {
			baseUri = config.baseUri();
		}
	}


	public TriplestoreStorage(String endpoint, String user, String pwd, String baseUri) {
		this.endpoint = endpoint;
		this.user=user;
		this.password =pwd;
		if(baseUri!=null && !baseUri.isEmpty()) {
			this.baseUri=baseUri;
		}
	}

	public TriplestoreStorage(String endpoint, String baseUri) {
		this.endpoint = endpoint;
		if(baseUri!=null && !baseUri.isEmpty()){
			this.baseUri=baseUri;
		}
		user = null;
		password = null;
	}

	public TriplestoreStorage(String endpoint) {
		this.endpoint = endpoint;
		user = null;
		password = null;
	}

	@Override
	public void storeResult(Model data) {
		super.storeResult(data);
		if (metricResults.size() == 0)
			return;

		StringWriter results = new StringWriter();
		RDFDataMgr.write(results, metricResults, Lang.NT);
		String update = "INSERT DATA {" + results.toString() + "}";
		//Create Update Request from block
		blockRequest.add(update);

		//submit Block to Triple Store
		UpdateProcessor processor = UpdateExecutionFactory
				.createRemote(blockRequest, endpoint, createHttpClient());
		processor.execute();
		blockRequest = new UpdateRequest();
	}



	private HttpClient createHttpClient(){
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		if(user !=null && password !=null){
			Credentials credentials = new UsernamePasswordCredentials(user, password);
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
