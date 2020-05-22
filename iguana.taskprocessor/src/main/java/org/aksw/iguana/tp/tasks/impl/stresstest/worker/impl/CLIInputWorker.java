package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.tp.config.CONSTANTS;
import org.aksw.iguana.tp.model.QueryExecutionStats;
import org.aksw.iguana.tp.utils.CLIProcessManager;
import org.aksw.iguana.tp.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

public class CLIInputWorker extends CLIBasedWorker {

	private int currentQueryID;
	private Random queryPatternChooser;
	private Process process;
	private String initFinished;
	private String queryFinished;
	private String error;

	public CLIInputWorker() {
		super("CLIInputWorker");
	}

	public CLIInputWorker(String[] args) {
		super(args, "CLIInputWorker");
		queryPatternChooser = new Random(this.workerID);

	}

	@Override
	public void init(Properties p) {
		super.init(p);
		this.initFinished = p.getProperty(CONSTANTS.CLI_INIT_FINISHED);
		this.queryFinished = p.getProperty(CONSTANTS.CLI_QUERY_FINISHED);
		this.error = p.getProperty(CONSTANTS.CLI_ERROR);
		this.setWorkerProperties();
	}

	@Override
	public void init(String args[]) {
		super.init(args);
		this.initFinished = args[10];
		this.queryFinished = args[11];
		this.error = args[12];
		this.setWorkerProperties();
	}

	private void setWorkerProperties()
	{
		queryPatternChooser = new Random(this.workerID);

		// Create a CLI process, initialize it
		System.out.println("Init CLIInputWorker " + this.queryFinished);
		this.process = CLIProcessManager.createProcess(this.service);
		try {
			CLIProcessManager.countLinesUntilStringOccurs(process, this.initFinished, this.error); //Init
		} catch (IOException e) {
			e.printStackTrace();
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
						System.out.println("Process Alive: " + process.isAlive());
						System.out.println("Reader ready: " + CLIProcessManager.isReaderReady(process));
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
			System.out.println("[DEBUG] Query successfully executed size: " + size.get());
			super.addResults(new QueryExecutionStats (queryID, COMMON.QUERY_SUCCESS, duration, size.get() ));
			return;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
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
		int queryLine = queryPatternChooser.nextInt(queriesInFile);
		queryStr.append(FileUtils.readLineAt(queryLine, currentQueryFile));

		// If there is no more query(Pattern) start from beginning.
		if (this.currentQueryID >= this.queryFileList.length) {
			this.currentQueryID = 0;
		}

	}

	@Override
	public void setQueriesList(File[] queries) {
		super.setQueriesList(queries);
		this.currentQueryID = queryPatternChooser.nextInt(this.queryFileList.length);
	}

	@Override
	public void stopSending() {
		super.stopSending();
		process.destroyForcibly();
	}
}
