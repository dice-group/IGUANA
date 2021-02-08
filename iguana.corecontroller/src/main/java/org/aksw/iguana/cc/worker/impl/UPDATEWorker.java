package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.query.set.QuerySet;
import org.aksw.iguana.cc.worker.impl.update.UpdateTimer;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;

import java.io.IOException;
import java.time.Instant;
import java.util.Properties;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

/**
 * 
 * A Worker using SPARQL Updates to create service request.
 * 
 * @author f.conrads
 *
 */
@Shorthand("UPDATEWorker")
public class UPDATEWorker extends HttpPostWorker {

	private int currentQueryID = 0;
	private UpdateTimer updateTimer = new UpdateTimer();
	private String timerStrategy;

	public UPDATEWorker(String taskID, Connection connection, String queriesFile, @Nullable String timerStrategy, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, Integer workerID) {
		super(taskID, connection, queriesFile, "application/sparql-update", null, null, "lang.SPARQL", timeOut, timeLimit, fixedLatency, gaussianLatency, "UPDATEWorker", workerID);
		this.timerStrategy=timerStrategy;
	}

	@Override
	public void startWorker(){
		setUpdateTimer(this.timerStrategy);
		super.startWorker();
	}

	@Override
	public void waitTimeMs() {
		double wait = this.updateTimer.calculateTime(durationInMilliseconds(startTime, Instant.now()), this.tmpExecutedQueries);
		LOGGER.debug("Worker[{{}} : {{}}]: Time to wait for next Query {{}}", workerType, workerID, wait);
		try {
			Thread.sleep((long)wait);
		} catch (InterruptedException e) {
			LOGGER.error("Worker[{{}} : {{}}]: Could not wait time before next query due to: {{}}", workerType,
					workerID, e);
			LOGGER.error("", e);
		}
		super.waitTimeMs();
	}

	@Override
	public synchronized void addResults(QueryExecutionStats results)
	{
			// create Properties store it in List
			Properties result = new Properties();
			result.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, this.taskID);
			result.put(COMMON.RECEIVE_DATA_TIME, results.getExecutionTime());
			result.put(COMMON.RECEIVE_DATA_SUCCESS, results.getResponseCode());
			result.put(COMMON.RECEIVE_DATA_SIZE, results.getResultSize());
			result.put(COMMON.QUERY_HASH, queryHash);
			result.setProperty(COMMON.FULL_QUERY_ID_KEY, results.getQueryID());
			result.put(COMMON.PENALTY, this.timeOut);
			// Add extra Meta Key, worker ID and worker Type
			result.put(COMMON.EXTRA_META_KEY, this.extra);
			setResults(result);
			executedQueries++;


	}

	@Override
	public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
		// If there is no more update send end signal, as their is nothing to do anymore
		if (this.currentQueryID >= this.queryFileList.length) {
			this.stopSending();
			return;
		}
		// get next Query File and next random Query out of it.
		QuerySet currentQueryFile = this.queryFileList[this.currentQueryID++];
		queryID.append(currentQueryFile.getName());

		queryStr.append(currentQueryFile.getContent());

	}

	@Override
	public void setQueriesList(QuerySet[] updateFiles) {
		super.setQueriesList(updateFiles);
	}

	/**
	 * Sets Update Timer according to strategy
	 * 
	 * @param strategyStr
	 *            The String representation of a UpdateTimer.Strategy
	 */
	private void setUpdateTimer(String strategyStr) {
		if (strategyStr == null)
			return;
		UpdateTimer.Strategy strategy = UpdateTimer.Strategy.valueOf(strategyStr.toUpperCase());
		switch (strategy) {
		case FIXED:
			if (timeLimit != null) {
				this.updateTimer = new UpdateTimer(this.timeLimit/this.queryFileList.length);
			} else {
				LOGGER.warn("Worker[{{}} : {{}}]: FIXED Updates can only be used with timeLimit!", workerType,
						workerID);
			}
			break;
		case DISTRIBUTED:
			if (timeLimit != null) {
				this.updateTimer = new UpdateTimer(this.queryFileList.length, (double) this.timeLimit);
			} else {
				LOGGER.warn("Worker[{{}} : {{}}]: DISTRIBUTED Updates can only be used with timeLimit!", workerType,
						workerID);
			}
			break;
		default:
			break;
		}
		LOGGER.debug("Worker[{{}} : {{}}]: UpdateTimer was set to UpdateTimer:{{}}", workerType, workerID, updateTimer);
	}



	/**
	 * Checks if one queryMix was already executed, as it does not matter how many mixes should be executed
	 * @param noOfQueryMixes
	 * @return
	 */
	@Override
	public boolean hasExecutedNoOfQueryMixes(double noOfQueryMixes){
		return getExecutedQueries() / (getNoOfQueries() * 1.0) >= 1;
	}

}
