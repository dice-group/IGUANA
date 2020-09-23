package org.aksw.iguana.cc.tasks.impl.stresstest.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.cc.utils.FileUtils;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.Random;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

@Shorthand("CLIWorker")
public class CLIWorker extends AbstractWorker {

	private Logger LOGGER = LoggerFactory.getLogger(getClass());


	private int currentQueryID;
	private Random queryChooser;

	public CLIWorker(String taskID, Connection connection, String queriesFile, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, Integer workerID) {
		super(taskID, connection, queriesFile, timeOut, timeLimit, fixedLatency, gaussianLatency, "CLIWorker", workerID);
		queryChooser = new Random(this.workerID);
	}


	@Override
	public void executeQuery(String query, String queryID) {
		Instant start = Instant.now();
		// use cli as service
		String q = "";
		try {
			q = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			LOGGER.error("Could not encode Query", e1);
		}
		String queryCLI = this.con.getEndpoint().replace("$QUERY$", query);
		queryCLI = queryCLI.replace("$ENCODEDQUERY$", q);

		if (this.con.getUser() != null) {
			queryCLI = queryCLI.replace("$USER$", this.con.getUser());
		}
		if (this.con.getPassword() != null) {
			queryCLI = queryCLI.replace("$PASSWORD$", this.con.getPassword());
		}
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

	@Override
	public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
		// get next Query File and next random Query out of it.
		File currentQueryFile = this.queryFileList[this.currentQueryID++];
		queryID.append(currentQueryFile.getName());

		int queriesInFile = FileUtils.countLines(currentQueryFile);
		int queryLine = queryChooser.nextInt(queriesInFile);
		queryStr.append(FileUtils.readLineAt(queryLine, currentQueryFile));

		// If there is no more query(Pattern) start from beginning.
		if (this.currentQueryID >= this.queryFileList.length) {
			this.currentQueryID = 0;
		}

	}

	@Override
	public void setQueriesList(File[] queries) {
		super.setQueriesList(queries);
		this.currentQueryID = queryChooser.nextInt(this.queryFileList.length);
	}

}
