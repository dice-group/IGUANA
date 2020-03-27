package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import org.aksw.iguana.tp.config.CONSTANTS;
import org.aksw.iguana.tp.model.QueryExecutionStats;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.tp.utils.FileUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.*;
import java.time.Instant;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

public class CLIInputWorker extends AbstractWorker {

	private int currentQueryID;
	private Random queryPatternChooser;
	private Process process;
	private String queryFinished;
	private BufferedReader reader;
	private int currentProcess = 0;
	private BufferedWriter output;
	private Boolean useFileToQuery = false;
	private String error;
	private ProcessBuilder processBuilder;
	private String initFinished;

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

			output = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			readUntilStringOccurs(reader, initFinished);

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
					return new QueryExecutionStats ( 0L, durationInMilliseconds(start, Instant.now()) );
				} else {
					return new QueryExecutionStats ( 0L, durationInMilliseconds(start, Instant.now()) );
				}
			} finally {
				executor.shutdown();
				executor.awaitTermination((long)(double)this.timeOut, TimeUnit.MILLISECONDS);
			}
			double end = Instant.now().getNano() / 1000000d;
			double duration = durationInMilliseconds(start, Instant.now());

			if (duration >= timeOut) {
				return new QueryExecutionStats ( -1L, duration );
			} else if (failed.get()) {
				return new QueryExecutionStats ( 0L, duration );
			}
			System.out.println("[DEBUG] Query successfully executed size: " + size.get());
			return new QueryExecutionStats ( 1L, duration, size.get() );
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		// ERROR
		return new QueryExecutionStats ( 0L, durationInMilliseconds(start, Instant.now()) );
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
