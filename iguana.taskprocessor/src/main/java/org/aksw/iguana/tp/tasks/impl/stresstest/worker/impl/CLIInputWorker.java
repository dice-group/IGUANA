package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.tp.utils.FileUtils;
import org.apache.commons.lang.SystemUtils;

public class CLIInputWorker extends AbstractWorker {

	private int currentQueryID;
	private Random queryPatternChooser;
	private Process process;
	private String queryFinished;
	private BufferedReader reader;
	private BufferedWriter output;

	public CLIInputWorker() {
		super("CLIWorker");
	}

	public CLIInputWorker(String[] args) {
		super(args, "CLIWorker");
		queryPatternChooser = new Random(this.workerID);

	}

	@Override
	public void init(String args[]) {
		super.init(args);
		String initFinished = args[10];
		this.queryFinished = args[11];
		queryPatternChooser = new Random(this.workerID);
		// start cli input
		ProcessBuilder processBuilder = new ProcessBuilder();
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

	private long readUntilStringOccurs(BufferedReader reader2, String initFinished) throws IOException {
		String line;
		long size=-1;
			while ((line = reader.readLine()) != null) {
				if(line.contains(initFinished)) {
					break;
				}
				size++;
			}
	
		return size;
	}

	@Override
	public Long[] getTimeForQueryMs(String query, String queryID) {
		long start = System.currentTimeMillis();
		// execute queryCLI and read response
		try {
			AtomicLong size =new AtomicLong(-1);
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.execute(new Runnable() {

				@Override
				public void run() {
					try {
						size.set(readUntilStringOccurs(reader, queryFinished));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}});
			output.write(query+"\n");
			output.flush();
			executor.shutdown();
			executor.awaitTermination(this.timeOut, TimeUnit.MILLISECONDS);
			long end = System.currentTimeMillis();
			if(end-start>timeOut) {
				return new Long[] { 0L, end - start };
			}
			System.out.println("[DEBUG] Query successfully executed size: "+size.get());
			return new Long[] { 1L, end - start , size.get()};
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		// ERROR
		return new Long[] { 0L, System.currentTimeMillis() - start };
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

}
