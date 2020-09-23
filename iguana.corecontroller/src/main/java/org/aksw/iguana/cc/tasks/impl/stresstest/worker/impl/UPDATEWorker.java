package org.aksw.iguana.cc.tasks.impl.stresstest.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.lang.impl.SPARQLLanguageProcessor;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.tasks.impl.stresstest.worker.impl.update.UpdateTimer;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.jena.sparql.modify.UpdateProcessRemote;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

/**
 * 
 * A Worker using SPARQL Updates to create service request.
 * 
 * @author f.conrads
 *
 */
@Shorthand("UPDATEWorker")
public class UPDATEWorker extends HttpWorker {

	private int currentQueryID = 0;
	private UpdateTimer updateTimer = new UpdateTimer();


	public UPDATEWorker(String taskID, Connection connection, String queriesFile,@Nullable String timerStrategy, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, Integer workerID) {
		super(taskID, connection, queriesFile, timeOut, timeLimit, fixedLatency, gaussianLatency, "UPDATEWorker", workerID);
		resultProcessor = new SPARQLLanguageProcessor();
		setUpdateTimer(timerStrategy);
	}


	@Override
	public void waitTimeMs() {
		double wait = this.updateTimer.calculateTime(durationInMilliseconds(startTime, Instant.now()), this.executedQueries);
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
	public void executeQuery(String query, String queryID) {
		UpdateRequest update = UpdateFactory.create(query);

		// Set update timeout
		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(this.timeOut.intValue())
				.setConnectTimeout(this.timeOut.intValue()).setSocketTimeout(this.timeOut.intValue()).build();
		CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

		// create Update Processor and use timeout config
		UpdateProcessor exec = UpdateExecutionFactory.createRemote(update, con.getUpdateEndpoint(), client);
		setCredentials(exec);
		Instant start = Instant.now();

		try {
			// Execute Update
			exec.execute();
			double duration = durationInMilliseconds(start, Instant.now());
			LOGGER.debug("Worker[{{}} : {{}}]: Update with ID {{}} took {{}}.", this.workerType, this.workerID, queryID,
					duration);
			// Return time
			super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_SUCCESS, duration));
			return;
		} catch (Exception e) {
			LOGGER.warn("Worker[{{}} : {{}}]: Could not execute the following update\n{{}}\n due to", this.workerType,
					this.workerID, query, e);
		}
		// Exception was thrown, return error
		//return -1L;
		super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now())));
	}

	private void setCredentials(UpdateProcessor exec) {
		if (exec instanceof UpdateProcessRemote && con.getUser() != null && !con.getUser().isEmpty() && con.getPassword() != null
				&& !con.getPassword().isEmpty()) {
			CredentialsProvider provider = new BasicCredentialsProvider();

			provider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
					new UsernamePasswordCredentials(con.getUser(), con.getPassword()));
			HttpContext httpContext = new BasicHttpContext();
			httpContext.setAttribute(HttpClientContext.CREDS_PROVIDER, provider);

			((UpdateProcessRemote) exec).setHttpContext(httpContext);
			HttpClient test = ((UpdateProcessRemote) exec).getClient();
			System.out.println(test);
		}

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

	@Override
	public void setQueriesList(File[] updateFiles) {
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
		UpdateTimer.Strategy strategy = UpdateTimer.Strategy.valueOf(strategyStr);
		switch (strategy) {
		case FIXED:
			if (timeLimit != null) {
				this.updateTimer = new UpdateTimer(this.queryFileList.length / this.timeLimit);
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
	 * Returns list of updates
	 * 
	 * @return
	 */
	public File[] getUpdateFiles() {
		return this.queryFileList;
	}

}
