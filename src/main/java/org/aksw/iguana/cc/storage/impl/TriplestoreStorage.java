package org.aksw.iguana.cc.storage.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.cc.config.elements.StorageConfig;
import org.aksw.iguana.cc.controller.MainController;
import org.aksw.iguana.cc.storage.Storage;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.exec.http.UpdateExecutionHTTP;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.http.HttpClient;


/**
 * This Storage will save all the metric results into a specified triple store
 *
 * @author f.conrads
 *
 */
public class TriplestoreStorage implements Storage {

	Logger logger = LoggerFactory.getLogger(TriplestoreStorage.class);

	public record Config(
			@JsonProperty(required = true) String endpoint,
			String user,
			String password,
			String baseUri
	) implements StorageConfig {}

	private UpdateRequest blockRequest = UpdateFactory.create();
	private final String endpoint;
	private final String user;
	private final String password;
	private final String baseUri;

	public TriplestoreStorage(Config config) {
		endpoint = config.endpoint();
		user = config.user();
		password = config.password();
		baseUri = config.baseUri();
	}


	public TriplestoreStorage(String endpoint, String user, String pwd, String baseUri) {
		this.endpoint = endpoint;
		this.user = user;
		this.password = pwd;
		this.baseUri = baseUri;
	}

	public TriplestoreStorage(String endpoint) {
		this.endpoint = endpoint;
		this.user = null;
		this.password = null;
		this.baseUri = null;
	}

	@Override
	public void storeResult(Model data) {
		StringWriter results = new StringWriter();
		RDFDataMgr.write(results, data, Lang.NT);
		String update = "INSERT DATA {" + results.toString() + "}";
		//Create Update Request from block
		blockRequest.add(update);

		//submit Block to Triple Store
		UpdateProcessor processor = UpdateExecutionHTTP.service(endpoint).update(blockRequest).httpClient(createHttpClient()).build();

		// If dry run is enabled, the data will not be sent to an existing triplestore,
		// therefore we catch the exception and log it instead of letting the program crash.
		// The dry run is used for generating the configuration files for the native compilation with GraalVM.
		// For normal runs, exceptions will be thrown normally.
		if (MainController.Args.dryRun) {
			try {
				processor.execute();
			} catch (Exception e) {
				logger.error("Error while storing data in triplestore: " + e.getMessage());
			}
		} else {
			processor.execute();
		}
		blockRequest = new UpdateRequest();
	}

	private HttpClient createHttpClient() {
		final var httpClient = HttpClient.newBuilder();
		if(user != null && password != null){
			httpClient.authenticator(new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(user, password.toCharArray());
				}
			});
		}
		return httpClient.build();
	}

	@Override
	public String toString(){
		return this.getClass().getSimpleName();
	}
}
