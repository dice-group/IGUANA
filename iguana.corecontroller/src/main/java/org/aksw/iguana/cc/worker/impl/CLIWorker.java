package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.worker.AbstractRandomQueryChooserWorker;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

/**
 * Worker to execute a query again a CLI process, the connection.service will be the command to execute the query against.
 *
 * command may look like the following<br></br>
 * cliprocess.sh $QUERY$ $USER$ $PASSWORD$
 * </br>
 * whereas $QUERY$ will be exchanged with the actual query as well as user and password.
 * Further on it is possible to encode the query using $ENCODEDQUERY$ instead of $QUERY$
 *
 */
@Shorthand("CLIWorker")
public class CLIWorker extends AbstractRandomQueryChooserWorker {

	private Logger LOGGER = LoggerFactory.getLogger(getClass());


	public CLIWorker(String taskID, Connection connection, String queriesFile, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, Integer workerID) {
		super(taskID, connection, queriesFile, timeOut, timeLimit, fixedLatency, gaussianLatency, "CLIWorker", workerID);
	}


	@Override
	public void executeQuery(String query, String queryID) {
		Instant start = Instant.now();
		// use cli as service
		String encodedQuery = "";
		try {
			encodedQuery = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			LOGGER.error("Could not encode Query", e1);
		}
		String queryCLI = getReplacedQuery(query, encodedQuery);
		// execute queryCLI and read response
		ProcessBuilder processBuilder = new ProcessBuilder().redirectErrorStream(true);
		processBuilder.command(new String[] { "bash", "-c", queryCLI });
		try {

			Process process = processBuilder.start();

			StringBuilder output = new StringBuilder();
			long size = -1;

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

				String line;
				// -1 as the first line should be the header
				while ((line = reader.readLine()) != null) {

					output.append(line + "\n");
					size++;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			int exitVal = process.waitFor();
			if (exitVal == 0) {
				LOGGER.debug("Query successfully executed size: {}", size);
			} else {
				LOGGER.warn("Exit Value of Process was not 0, was {} ", exitVal);
				super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now()) ));
				return;
			}
			super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_SUCCESS, durationInMilliseconds(start, Instant.now()), size ));
			return;
		} catch (Exception e) {
			LOGGER.warn("Unknown Exception while executing query", e);
		}
		// ERROR
		super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now()) ));
	}

	private String getReplacedQuery(String query, String encodedQuery) {
		String queryCLI = this.con.getEndpoint().replace("$QUERY$", query);
		queryCLI = queryCLI.replace("$ENCODEDQUERY$", encodedQuery);

		if (this.con.getUser() != null) {
			queryCLI = queryCLI.replace("$USER$", this.con.getUser());
		}
		else{
			queryCLI = queryCLI.replace("$USER$", "");

		}
		if (this.con.getPassword() != null) {
			queryCLI = queryCLI.replace("$PASSWORD$", this.con.getPassword());
		}
		else{
			queryCLI = queryCLI.replace("$PASSWORD$", "");

		}
		return queryCLI;

	}


}
