package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Worker to execute a query against a CLI process, the connection.service will be the command to execute the query against.
 * <p>
 * Assumes that the CLI process won't stop but will just accepts queries one after another and returns the results in the CLI output.
 * also assumes that the query has to be read from a file instead of plain input
 * <p>
 * This worker can be set to be created multiple times in the background if one process will throw an error, a backup process was already created and can be used.
 * This is handy if the process won't just prints an error message, but simply exits.
 */
@Shorthand("CLIInputFileWorker")
public class CLIInputFileWorker extends MultipleCLIInputWorker {
    private final String dir;

    public CLIInputFileWorker(String taskID, Integer workerID, Connection connection, Map<String, Object> queries, @Nullable Integer timeLimit, @Nullable Integer timeOut, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, String initFinished, String queryFinished, String queryError, @Nullable Integer numberOfProcesses, String directory) {
        super(taskID, workerID, connection, queries, timeLimit, timeOut, fixedLatency, gaussianLatency, initFinished, queryFinished, queryError, numberOfProcesses);
        this.dir = directory;
    }

    @Override
    protected String writableQuery(String query) {
        File f;

        try {
            new File(this.dir).mkdirs();
            f = new File(this.dir + File.separator + "tmpquery.sparql");
            f.createNewFile();
            f.deleteOnExit();
            try (PrintWriter pw = new PrintWriter(f)) {
                pw.print(query);
            }
            return f.getName();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return query;
    }

}
