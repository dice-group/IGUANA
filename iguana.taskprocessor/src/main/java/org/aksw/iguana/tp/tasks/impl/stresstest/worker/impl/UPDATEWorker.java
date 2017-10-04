package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Properties;
import org.aksw.iguana.tp.config.CONSTANTS;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.update.UpdateComparator;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.update.UpdateStrategy;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.update.UpdateTimer;
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
	private UpdateStrategy updateStrategy;
	private UpdateTimer updateTimer = new UpdateTimer();

	public UPDATEWorker() {
	}

	public UPDATEWorker(String taskID, String workerID, String workerType, String timeLimit, String service, String timeOut,
			String updateFolder, String fixedLatency, String gaussianLatency, String updateStrategy, String timerStrategy) {
		this(taskID, Integer.parseInt(workerID), workerType, Long.parseLong(timeLimit), service, Long.parseLong(timeOut),
				updateFolder, Integer.parseInt(fixedLatency), Integer.parseInt(gaussianLatency), updateStrategy, timerStrategy);
	}
	
	public UPDATEWorker(String taskID, int workerID, String workerType, Long timeLimit, String service, Long timeOut,
			String updateFolder, Integer fixedLatency, Integer gaussianLatency, String updateStrategy, String timerStrategy) {
		super(taskID, workerID, workerType, timeLimit, updateFolder, fixedLatency, gaussianLatency);
		this.service = service;
		// set default timeout to 180s
		this.timeOut = 180000L;
		// if timeout is set, set timeout
		if (timeOut != null)
			this.timeOut = timeOut;

		// set default updateStrategy to none
		this.updateStrategy = UpdateStrategy.NONE;
		// if updateStrategy is set, set updateStrategy
		if (updateStrategy != null) {
			this.updateStrategy = UpdateStrategy.valueOf(updateStrategy);
		}

		setUpdateTimer(timerStrategy);
	}

	@Override
	public void init(Properties p) {
		// At first call init from AbstractWorker!
		super.init(p);
		this.service = p.getProperty(CONSTANTS.SPARQL_CURRENT_ENDPOINT);
		this.timeOut = (long) p.getOrDefault(CONSTANTS.SPARQL_TIMEOUT, 180000);
		String timerStrategy = p.getProperty(CONSTANTS.STRESSTEST_UPDATE_TIMERSTRATEGY);
		//use updateStrategy if set, otherwise use default: none
		this.updateStrategy = UpdateStrategy.valueOf(p.getOrDefault(CONSTANTS.STRESSTEST_UPDATE_STRATEGY, "NONE").toString());
		
	
		setUpdateTimer(timerStrategy);
	}
	
	private void setUpdateTimer(String strategyStr) {
		if(strategyStr == null)
			return;
		UpdateTimer.Strategy strategy = UpdateTimer.Strategy.valueOf(strategyStr);
		switch(strategy) {
		case FIXED:
			this.updateTimer = new UpdateTimer();
		case DISTRIBUTED:
			if(timeLimit!=null) {
				this.updateTimer = new UpdateTimer(this.queryFileList.length, this.timeLimit);
			}
			else {
				LOGGER.warn("Worker[{{}} : {{}}]: DISTRIBUTED Updates can only be used with timeLimit!", workerType, workerID);
			}
		default:
			break;
		}
		LOGGER.debug("Worker[{{}} : {{}}]: UpdateTimer was set to UpdateTimer:{{}}", workerType, workerID, updateTimer);
	}
	
	@Override
	public void waitTimeMs(){
		long currentTime = Calendar.getInstance().getTimeInMillis();
	   	long wait = this.updateTimer.calculateTime(currentTime - this.startTime, this.executedQueries);
	    LOGGER.debug("Worker[{{}} : {{}}]: Time to wait for next Query {{}}", workerType, workerID, wait);
	    try {
	    	Thread.sleep(wait);
	    } catch (InterruptedException e) {
	    	LOGGER.error("Worker[{{}} : {{}}]: Could not wait time before next query due to: {{}}", workerType, workerID, e);
	    	LOGGER.error("", e);
	    }
	    super.waitTimeMs();
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
		//TODO check if updateStrategy is NEXT
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
	
	@Override
	public void setQueriesList(File[] updateFiles) {
		super.setQueriesList(updateFiles);
		// sort updateFiles according to updateStrategy
		UpdateComparator cmp = new UpdateComparator(this.updateStrategy);
		Arrays.sort(this.queryFileList, cmp);
	}

}
