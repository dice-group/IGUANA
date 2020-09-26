package org.aksw.iguana.cc.worker.impl;


import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;

/**
 * Worker to execute a query against a CLI process, the connection.service will be the command to execute the query against.
 *
 * Assumes that the CLI process won't stop but will just accepts queries one after another and returns the results in the CLI output.
 * also assumes that the query has to be prefixed and suffixed.
 * For example: SPARQL SELECT * {?s ?p ?o} ; whereas 'SPARQL' is the prefix and ';' is the suffix.
 *
 * This worker can be set to be created multiple times in the background if one process will throw an error, a backup process was already created and can be used.
 * This is handy if the process won't just prints an error message, but simply exits.
 *
 */
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
