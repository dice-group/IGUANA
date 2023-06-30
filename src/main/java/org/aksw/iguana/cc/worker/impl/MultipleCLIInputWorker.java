package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.utils.CLIProcessManager;
import org.aksw.iguana.cc.worker.AbstractWorker;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

/**
 * Worker to execute a query against a CLI process, the connection.service will be the command to execute the query against.
 * <p>
 * Assumes that the CLI process won't stop but will just accepts queries one after another and returns the results in the CLI output.
 * <p>
 * This worker can be set to be created multiple times in the background if one process will throw an error, a backup process was already created and can be used.
 * This is handy if the process won't just prints an error message, but simply exits.
 */
@Shorthand("MultipleCLIInputWorker")
public class MultipleCLIInputWorker extends AbstractWorker {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final String initFinished;
    private final String queryFinished;
    private final String error;
    protected List<Process> processList;
    protected int currentProcessId = 0;
    protected int numberOfProcesses = 5;
    private Process currentProcess;

    public MultipleCLIInputWorker(String taskID, Integer workerID, ConnectionConfig connection, Map<String, Object> queries, @Nullable Integer timeLimit, @Nullable Integer timeOut, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, String initFinished, String queryFinished, String queryError, @Nullable Integer numberOfProcesses) {
        super(taskID, workerID, connection, queries, timeLimit, timeOut, fixedLatency, gaussianLatency);
        this.initFinished = initFinished;
        this.queryFinished = queryFinished;
        this.error = queryError;
        if (numberOfProcesses != null) {
            this.numberOfProcesses = numberOfProcesses;
        }
        this.setWorkerProperties();
    }

    private void setWorkerProperties() {
        // start cli input

        // Create processes, set first process as current process
        this.processList = CLIProcessManager.createProcesses(this.numberOfProcesses, this.con.getEndpoint());
        this.currentProcess = this.processList.get(0);

        // Make sure that initialization is complete
        for (Process value : this.processList) {
            try {
                CLIProcessManager.countLinesUntilStringOccurs(value, initFinished, error);
            } catch (IOException e) {
                LOGGER.error("Exception while trying to wait for init of CLI Process", e);
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
            executor.execute(() -> {
                try {
                    LOGGER.debug("Process Alive: {}", this.currentProcess.isAlive());
                    LOGGER.debug("Reader ready: {}", CLIProcessManager.isReaderReady(this.currentProcess));
                    size.set(CLIProcessManager.countLinesUntilStringOccurs(this.currentProcess, this.queryFinished, this.error));
                } catch (IOException e) {
                    failed.set(true);
                }
            });

            // Execute the query on the process
            try {
                if (this.currentProcess.isAlive()) {
                    CLIProcessManager.executeCommand(this.currentProcess, writableQuery(query));
                } else if (this.endSignal) {
                    super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now())));
                    return;
                } else {
                    setNextProcess();
                    super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now())));
                    return;
                }
            } finally {
                executor.shutdown();
                executor.awaitTermination((long) (double) this.timeOut, TimeUnit.MILLISECONDS);
            }

            // At this point, query is executed and background thread has processed the results.
            // Next, calculate time for benchmark.
            double duration = durationInMilliseconds(start, Instant.now());

            if (duration >= this.timeOut) {
                setNextProcess();
                super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_SOCKET_TIMEOUT, duration));
                return;
            } else if (failed.get()) {
                if (!this.currentProcess.isAlive()) {
                    setNextProcess();
                }
                super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, duration));
                return;
            }

            // SUCCESS
            LOGGER.debug("Query successfully executed size: {}", size.get());
            super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_SUCCESS, duration, size.get()));
        } catch (IOException | InterruptedException e) {
            LOGGER.warn("Exception while executing query ", e);
            // ERROR
            super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now())));
        }
    }

    private void setNextProcess() {
        int oldProcessId = this.currentProcessId;
        this.currentProcessId = this.currentProcessId == this.processList.size() - 1 ? 0 : this.currentProcessId + 1;

        // destroy old process
        CLIProcessManager.destroyProcess(this.currentProcess);
        if (oldProcessId == this.currentProcessId) {
            try {
                this.currentProcess.waitFor();
            } catch (InterruptedException e) {
                LOGGER.error("Process was Interrupted", e);
            }
        }

        // create and initialize new process to replace previously destroyed process
        Process replacementProcess = CLIProcessManager.createProcess(this.con.getEndpoint());
        try {
            CLIProcessManager.countLinesUntilStringOccurs(replacementProcess, this.initFinished, this.error); // Init
            this.processList.set(oldProcessId, replacementProcess);
        } catch (IOException e) {
            LOGGER.error("Process replacement didn't work", e);
        }

        // finally, update current process
        this.currentProcess = this.processList.get(this.currentProcessId);
    }

    protected String writableQuery(String query) {
        return query;
    }


    @Override
    public void stopSending() {
        super.stopSending();
        for (Process pr : this.processList) {
            pr.destroyForcibly();
            try {
                pr.waitFor();
            } catch (InterruptedException e) {
                LOGGER.error("Process waitFor was Interrupted", e);
            }
        }
    }
}
