package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Worker to execute a query against a CLI process, the connection.service will be the command to execute the query against.
 *
 * Assumes that the CLI process won't stop but will just accepts queries one after another and returns the results in the CLI output.
 * also assumes that the query has to be read from a file instead of plain input
 *
 * This worker can be set to be created multiple times in the background if one process will throw an error, a backup process was already created and can be used.
 * This is handy if the process won't just prints an error message, but simply exits.
 *
 */
@Shorthand("CLIInputFileWorker")
public class CLIInputFileWorker extends MultipleCLIInputWorker {


	private String dir;

	public CLIInputFileWorker(String taskID, Connection connection, String queriesFile, String initFinished, String queryFinished, String queryError, @Nullable Integer numberOfProcesses, String directory, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, Integer workerID) {
		super(taskID, connection, queriesFile, initFinished,queryFinished,queryError, numberOfProcesses,timeOut, timeLimit, fixedLatency, gaussianLatency, "CLIInputFileWorker", workerID);
		this.dir = directory;
	}
	
	@Override
	protected String writableQuery(String query) {
		File f;
		
		try {
			f = new File(dir+File.separator+"tmpquery.sparql");
			f.deleteOnExit();
			try(PrintWriter pw = new PrintWriter(f)){
				pw.print(query);
			}
			return f.getName();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return query;
	}
	
}
