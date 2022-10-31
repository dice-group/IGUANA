package org.aksw.iguana.cc.worker.impl;


import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;

import java.util.Map;

/**
 * Worker to execute a query against a CLI process, the connection.service will be the command to execute the query against.
 * <p>
 * Assumes that the CLI process won't stop but will just accepts queries one after another and returns the results in the CLI output.
 * also assumes that the query has to be prefixed and suffixed.
 * For example: SPARQL SELECT * {?s ?p ?o} ; whereas 'SPARQL' is the prefix and ';' is the suffix.
 * <p>
 * This worker can be set to be created multiple times in the background if one process will throw an error, a backup process was already created and can be used.
 * This is handy if the process won't just prints an error message, but simply exits.
 */
@Shorthand("CLIInputPrefixWorker")
public class CLIInputPrefixWorker extends MultipleCLIInputWorker {

    private final String prefix;
    private final String suffix;

    public CLIInputPrefixWorker(String taskID, Integer workerID, Connection connection, Map<Object, Object> queries, @Nullable Integer timeLimit, @Nullable Integer timeOut, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, String initFinished, String queryFinished, String queryError, @Nullable Integer numberOfProcesses, String queryPrefix, String querySuffix) {
        super(taskID, workerID, connection, queries, timeLimit, timeOut, fixedLatency, gaussianLatency, initFinished, queryFinished, queryError, numberOfProcesses);
        this.prefix = queryPrefix;
        this.suffix = querySuffix;
    }

    @Override
    protected String writableQuery(String query) {
        return this.prefix + " " + query + " " + this.suffix;
    }

}
