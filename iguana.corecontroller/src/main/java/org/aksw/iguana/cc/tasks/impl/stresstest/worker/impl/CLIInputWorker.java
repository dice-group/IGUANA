package org.aksw.iguana.cc.tasks.impl.stresstest.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.utils.CLIProcessManager;
import org.aksw.iguana.cc.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

@Shorthand("CLIInputWorker")
public class CLIInputWorker extends AbstractWorker {

	private Logger LOGGER = LoggerFactory.getLogger(getClass());

	private int currentQueryID;
	private Random queryChooser;
	private Process process;
	private String initFinished;
	private String queryFinished;
	private String error;

	public CLIInputWorker(String taskID, Connection connection, String queriesFile, String initFinished, String queryFinished, String queryError, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, Integer workerID) {
		super(taskID, connection, queriesFile, timeOut, timeLimit, fixedLatency, gaussianLatency, "CLIInputWorker", workerID);
		queryChooser = new Random(this.workerID);
		this.initFinished = initFinished;
		this.queryFinished = queryFinished;
		this.error = queryError;
		this.setWorkerProperties();

	}

	private void setWorkerProperties()
	{
		queryChooser = new Random(this.workerID);

		// Create a CLI process, initialize it
		this.process = CLIProcessManager.createProcess(this.con.getEndpoint());
		try {
			CLIProcessManager.countLinesUntilStringOccurs(process, this.initFinished, this.error); //Init
		} catch (IOException e) {
			LOGGER.error("Exception while trying to wait for init of CLI Process",e);
		}
	}

	@Override
	public void executeQuery(String query, String queryID) {
		Instant start = Instant.now();

		try {
			// Create background thread that will watch the output of the process and prepare results
			AtomicLong size = new AtomicLong(-1);
			AtomicBoolean failed = new AtomicBoolean(false);
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.execute(new Runnable() {

				@Override
				public void run() {
					try {
						LOGGER.debug("Process Alive: {}", process.isAlive());
						LOGGER.debug("Reader ready: {}", CLIProcessManager.isReaderReady(process));
						size.set(CLIProcessManager.countLinesUntilStringOccurs(process, queryFinished, error));
					} catch (IOException e) {
						failed.set(true);
					}
				}
			});

			// Execute the query on the process
			try {
				if (process.isAlive()) {
					CLIProcessManager.executeCommand(process, writableQuery(query));
				} else if (this.endSignal) {
					super.addResults(new QueryExecutionStats (queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now()) ));
					return;
				} else {
					super.addResults(new QueryExecutionStats (queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now()) ));
					return;
				}
			} finally {
				executor.shutdown();
				executor.awaitTermination((long)(double)this.timeOut, TimeUnit.MILLISECONDS);
			}

			// At this point, query is executed and background thread has processed the results.
			// Next, calculate time for benchmark.
			double duration = durationInMilliseconds(start, Instant.now());

			if (duration >= timeOut) {
				super.addResults(new QueryExecutionStats (queryID, COMMON.QUERY_SOCKET_TIMEOUT, duration ));
				return;
			} else if (failed.get()) {
				super.addResults(new QueryExecutionStats (queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, duration ));
				return;
			}

			// SUCCESS
			LOGGER.debug("Query successfully executed size: {}", size.get());
			super.addResults(new QueryExecutionStats (queryID, COMMON.QUERY_SUCCESS, duration, size.get() ));
			return;
		} catch (IOException | InterruptedException e) {
			LOGGER.warn("Exception while executing query ",e);
			// ERROR
			super.addResults(new QueryExecutionStats (queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now()) ));
		}
	}


	protected String writableQuery(String query) {
		return query;
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

	@Override
	public void stopSending() {
		super.stopSending();
		process.destroyForcibly();
	}
}
