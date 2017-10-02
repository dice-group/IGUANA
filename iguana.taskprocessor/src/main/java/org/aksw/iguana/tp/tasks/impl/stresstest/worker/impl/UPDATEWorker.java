package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import org.aksw.iguana.tp.config.CONSTANTS;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.tp.utils.FileUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

/**
 * 
 * A Worker using SPARQL 1.1 to create service request.
 * 
 * @author f.conrads
 *
 */
public class UPDATEWorker extends AbstractWorker {

	private String service;
	private Long timeOut;
	private int currentQueryID = 0;

	public UPDATEWorker() {
	}

	public UPDATEWorker(String service, long timeOut, String taskID, int workerID, String workerType,
			File[] queryFileList, Integer fixedLatency, Integer gaussianLatency, String UpdateStrategy) {
		super(taskID, workerID, workerType, queryFileList, fixedLatency, gaussianLatency);
		this.service = service;
		this.timeOut = timeOut;
		// TODO get UpdateStrategy of properties, sort Files according to UpdateStrategyComparator

	}

	@Override
	public void init(Properties p) {
		// At first call init from AbstractWorker!
		super.init(p);
		this.service = p.getProperty(CONSTANTS.SPARQL_CURRENT_ENDPOINT);
		this.timeOut = (long) p.getOrDefault(CONSTANTS.SPARQL_TIMEOUT, 180000);
		// TODO get UpdateStrategy of properties, sort Files according to UpdateStrategyComparator
		
	}

	@Override
	public long getTimeForQueryMs(String query, String queryID) {
		UpdateRequest update = UpdateFactory.create(query);

		// Set update timeout
		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(this.timeOut.intValue())
				.setConnectTimeout(this.timeOut.intValue()).setSocketTimeout(this.timeOut.intValue()).build();
		CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

		// create Update Processor and use timeout config
		UpdateProcessor exec = UpdateExecutionFactory.createRemote(update, service, client);

		try {
			long start = System.currentTimeMillis();
			// Execute Update
			exec.execute();
			long end = System.currentTimeMillis();
			LOGGER.debug("Worker[{{}} : {{}}]: Update with ID {{}} took {{}}.", this.workerType, this.workerID, queryID,
					end - start);
			// Return time
			return end - start;
		} catch (Exception e) {
			LOGGER.warn("Worker[{{}} : {{}}]: Could not execute the following update\n{{}}\n due to", this.workerType,
					this.workerID, query, e);
		}
		// Exception was thrown, return error
		return -1L;
	}

	@Override
	public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
		// If there is no more update send end signal, as their is nothing to do anymore
		if (this.currentQueryID >= this.queryFileList.length) {
			this.stopSending();
			return;
		}
		// get next Query File and next random Query out of it.
		File currentQueryFile = this.queryFileList[this.currentQueryID++];
		queryID.append(currentQueryFile.getName());

		queryStr.append(org.apache.commons.io.FileUtils.readFileToString(currentQueryFile, "UTF-8"));

	}

}
