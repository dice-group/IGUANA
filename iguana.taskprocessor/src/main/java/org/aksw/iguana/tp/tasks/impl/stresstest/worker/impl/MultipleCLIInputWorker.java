package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.tp.config.CONSTANTS;
import org.aksw.iguana.tp.model.QueryExecutionStats;
import org.aksw.iguana.tp.utils.CLIProcessManager;
import org.aksw.iguana.tp.utils.FileUtils;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

public class MultipleCLIInputWorker extends CLIBasedWorker {

	private int currentQueryID;
	private Random queryPatternChooser;
	private Process currentProcess;
	protected List<Process> processList;
	private int currentProcessId = 0;
	private String initFinished;
	private String queryFinished;
	private String error;
	protected int numberOfProcesses;

	public MultipleCLIInputWorker() {
		super("CLIInputWorker");
	}

	public MultipleCLIInputWorker(String[] args) {
		super(args, "CLIInputWorker");
		queryPatternChooser = new Random(this.workerID);

	}

	@Override
	public void init(Properties p) {
		super.init(p);
		this.initFinished = p.getProperty(CONSTANTS.CLI_INIT_FINISHED);
		this.queryFinished = p.getProperty(CONSTANTS.CLI_QUERY_FINISHED);
		this.error = p.getProperty(CONSTANTS.CLI_ERROR);
		this.numberOfProcesses = Integer.parseInt(p.getOrDefault(CONSTANTS.NO_OF_PROCESSES, new Integer(5)).toString());
		this.setWorkerProperties();
	}

	private void setWorkerProperties() {
		queryPatternChooser = new Random(this.workerID);
		// start cli input
		System.out.println("Init CLIInputWorker " + this.queryFinished);

		// Create processes, set first process as current process
		this.processList = CLIProcessManager.createProcesses(this.numberOfProcesses, this.service);
		this.currentProcess = processList.get(0);

		// Make sure that initialization is complete
		for (Process value : processList) {
			try {
				CLIProcessManager.countLinesUntilStringOccurs(value, initFinished, error);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	@Override
	public void executeQuery(String query, String queryID) {
		Instant start = Instant.now();
		// execute queryCLI and read response
		try {
			// Create background thread that will watch the output of the process and prepare results
			AtomicLong size = new AtomicLong(-1);
			AtomicBoolean failed = new AtomicBoolean(false);
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.execute(new Runnable() {

				@Override
				public void run() {
					try {
						System.out.println("Process Alive: " + currentProcess.isAlive());
						System.out.println("Reader ready: " + CLIProcessManager.isReaderReady(currentProcess));
						size.set(CLIProcessManager.countLinesUntilStringOccurs(currentProcess, queryFinished, error));
					} catch (IOException e) {
						failed.set(true);
					}
				}
			});

			// Execute the query on the process
			try {
				if (currentProcess.isAlive()) {
					CLIProcessManager.executeCommand(currentProcess, writableQuery(query));
				} else if (this.endSignal) {
					super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now()) ));
					return;
				} else {
					setNextProcess();
					super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now()) ));
					return;
				}
			} finally {
				executor.shutdown();
				executor.awaitTermination((long) (double)this.timeOut, TimeUnit.MILLISECONDS);
			}

			// At this point, query is executed and background thread has processed the results.
			// Next, calculate time for benchmark.
			double duration = durationInMilliseconds(start, Instant.now());

			if (duration >= timeOut) {
				setNextProcess();
				super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_SOCKET_TIMEOUT, duration ));
				return;
			} else if (failed.get()) {
				if (!currentProcess.isAlive()) {
					setNextProcess();
				}
				super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, duration ));
				return;
			}

			// SUCCESS
			System.out.println("[DEBUG] Query successfully executed size: " + size.get());
			super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_SUCCESS, duration, size.get() ));
			return;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			// ERROR
			super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now()) ));
		}
	}

	private void setNextProcess() {
		int oldProcessId = currentProcessId;
		currentProcessId = currentProcessId == processList.size() -1 ? 0 : currentProcessId + 1;

		// destroy old process
		CLIProcessManager.destroyProcess(currentProcess);
		if(oldProcessId== currentProcessId) {
			try {
				currentProcess.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// create and initialize new process to replace previously destroyed process
		Process replacementProcess = CLIProcessManager.createProcess(this.service);
		try {
			CLIProcessManager.countLinesUntilStringOccurs(replacementProcess, initFinished, error); // Init
			processList.set(oldProcessId, replacementProcess);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// finally, update current process
		currentProcess = processList.get(currentProcessId);
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
		for (Process pr : processList) {
			pr.destroyForcibly();
			try {
				pr.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
