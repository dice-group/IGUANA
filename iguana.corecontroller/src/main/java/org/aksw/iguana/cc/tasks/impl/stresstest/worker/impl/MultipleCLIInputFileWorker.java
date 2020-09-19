package org.aksw.iguana.cc.tasks.impl.stresstest.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

@Shorthand("MultipleCLIInputFileWorker")
public class MultipleCLIInputFileWorker extends MultipleCLIInputWorker {


	private String dir;

	public MultipleCLIInputFileWorker(String taskID, Connection connection, String queriesFile, String initFinished, String queryFinished, String queryError, @Nullable Integer numberOfProcesses, String directory, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency,Integer workerID) {
		super(taskID, connection, queriesFile, initFinished,queryFinished,queryError, numberOfProcesses,timeOut, timeLimit, fixedLatency, gaussianLatency, "MultipleCLIInputFileWorker", workerID);
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
