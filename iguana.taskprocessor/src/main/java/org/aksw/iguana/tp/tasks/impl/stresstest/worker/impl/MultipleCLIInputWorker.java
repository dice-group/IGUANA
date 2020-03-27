package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.time.Instant;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.aksw.iguana.tp.config.CONSTANTS;
import org.aksw.iguana.tp.model.QueryExecutionStats;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.tp.utils.FileUtils;
import org.apache.commons.lang.SystemUtils;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

public class MultipleCLIInputWorker extends AbstractWorker {

	private int currentQueryID;
	private Random queryPatternChooser;
	private Process process;
	protected Process[] processList = new Process[5];
	private String queryFinished;
	private BufferedReader reader;
	private int currentProcess = 0;
	private BufferedWriter output;
	private Boolean useFileToQuery = false;
	private String error;
	private ProcessBuilder processBuilder;
	private String initFinished;

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

	private void setWorkerProperties() {
		queryPatternChooser = new Random(this.workerID);
		// start cli input
		System.out.println("Init CLIInputWorker " + this.queryFinished);

		processBuilder = new ProcessBuilder();
		processBuilder.redirectErrorStream(true);
		try {
			if (SystemUtils.IS_OS_LINUX) {

				processBuilder.command(new String[] { "bash", "-c", this.service });

			} else if (SystemUtils.IS_OS_WINDOWS) {
				processBuilder.command(new String[] { "cmd.exe", "-c", this.service });
			}
			process = processBuilder.start();
			processList[0] = process;
			output = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			readUntilStringOccurs(reader, initFinished);
			for (int i = 1; i < processList.length; i++) {
				processList[i] = processBuilder.start();
				BufferedReader reader2 = new BufferedReader(new InputStreamReader(processList[i].getInputStream()));
				readUntilStringOccurs(reader2, initFinished);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private long readUntilStringOccurs(BufferedReader reader, String initFinished) throws IOException {
		String line;
		System.out.println("Will look for: " + initFinished + " or as error: " + error);
		StringBuilder output = new StringBuilder();
		long size = -1;
		while ((line = reader.readLine()) != null) {
			if (line.contains(error)) {
				System.out.println("Found error");
				System.out.println("Query finished with " + initFinished);

				throw new IOException(line);
			} else if (line.contains(initFinished)) {
				System.out.println("Query finished with " + initFinished);
				break;
			}
			if (output.length() < 1000) {
				output.append(line).append("\n");
			}
			size++;
		}
		System.out.println(output.substring(0, Math.min(1000, output.length())));
		return size;
	}

	@Override
	public QueryExecutionStats executeQuery(String query, String queryID) {
		Instant start = Instant.now();
		// execute queryCLI and read response
		try {
			AtomicLong size = new AtomicLong(-1);
			AtomicBoolean failed = new AtomicBoolean(false);
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.execute(new Runnable() {

				@Override
				public void run() {
					try {
						System.out.println("Process Alive: " + process.isAlive());
						System.out.println("Reader ready: " + reader.ready());
						size.set(readUntilStringOccurs(reader, queryFinished));
					} catch (IOException e) {
						failed.set(true);
					}
				}
			});
			BufferedWriter output = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
			try {
				if (process.isAlive()) {
					output.write(writableQuery(query) + "\n");
					output.flush();
				} else if (this.endSignal) {
					return new QueryExecutionStats( 0L, durationInMilliseconds(start, Instant.now()) );
				} else {
					setNextProcess();
					return new QueryExecutionStats( 0L, durationInMilliseconds(start, Instant.now()) );
				}
			} finally {
				executor.shutdown();
				executor.awaitTermination((long) (double)this.timeOut, TimeUnit.MILLISECONDS);
			}
			double duration = durationInMilliseconds(start, Instant.now());

			if (duration >= timeOut) {
				setNextProcess();
				return new QueryExecutionStats( 0L, duration );
			} else if (failed.get()) {
				if (!process.isAlive()) {
					setNextProcess();
				}
				return new QueryExecutionStats( 0L, duration );
			}
			System.out.println("[DEBUG] Query successfully executed size: " + size.get());
			return new QueryExecutionStats( 1L, duration, size.get() );
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		// ERROR
		return new QueryExecutionStats( 0L, durationInMilliseconds(start, Instant.now()) );
	}

	private void setNextProcess() {
		int oldProcess = currentProcess;
		currentProcess = currentProcess == processList.length-1 ? 0 : currentProcess + 1;
		// destroy old process;
		process.destroyForcibly();
		if(oldProcess==currentProcess) {
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// create new one for old in list
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					processList[oldProcess] = processBuilder.start();
					BufferedReader reader2 = new BufferedReader(
							new InputStreamReader(processList[oldProcess].getInputStream()));
					readUntilStringOccurs(reader2, initFinished);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		executor.shutdown();
		// get all from the next process
		process = processList[currentProcess];
		output = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
		reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
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
