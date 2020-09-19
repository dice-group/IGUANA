package org.aksw.iguana.cc.tasks.impl.stresstest.worker.impl;


import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;


@Shorthand("CLIInputPrefixWorker")
public class CLIInputPrefixWorker extends MultipleCLIInputWorker {

	private String prefix;
	private String suffix;

	public CLIInputPrefixWorker(String taskID, Connection connection, String queriesFile, String initFinished, String queryFinished, String queryError, @Nullable Integer numberOfProcesses, String queryPrefix, String querySuffix, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, Integer workerID) {
		super(taskID, connection, queriesFile, initFinished,queryFinished,queryError, numberOfProcesses,timeOut, timeLimit, fixedLatency, gaussianLatency, "CLIInputPrefixWorker", workerID);
		this.prefix=queryPrefix;
		this.suffix=querySuffix;
	}

	@Override
	protected String writableQuery(String query) {
		return prefix+" "+query+" "+suffix;
	}

}
