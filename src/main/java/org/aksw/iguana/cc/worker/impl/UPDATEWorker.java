package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.worker.impl.update.UpdateTimer;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

/**
 * A Worker using SPARQL Updates to create service request.
 *
 * @author f.conrads
 */
@Shorthand("UPDATEWorker")
public class UPDATEWorker extends HttpPostWorker {
    private final String timerStrategy;
    private UpdateTimer updateTimer = new UpdateTimer();

    private int queryCount;

    public UPDATEWorker(String taskID, Integer workerID, ConnectionConfig connection, Map<String, Object> queries, @Nullable Integer timeLimit, @Nullable Integer timeOut, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, @Nullable String timerStrategy) {
        super(taskID, workerID, connection, queries, timeLimit, timeOut, fixedLatency, gaussianLatency, null, null, "application/sparql-update");
        this.timerStrategy = timerStrategy;
    }

    @Override
    public void startWorker() {
        setUpdateTimer(this.timerStrategy);
        super.startWorker();
    }

    @Override
    public void waitTimeMs() {
        double wait = this.updateTimer.calculateTime(durationInMilliseconds(this.startTime, Instant.now()), this.tmpExecutedQueries);
        LOGGER.debug("Worker[{{}} : {{}}]: Time to wait for next Query {{}}", this.workerType, this.workerID, wait);
        try {
            Thread.sleep((long) wait);
        } catch (InterruptedException e) {
            LOGGER.error("Worker[{{}} : {{}}]: Could not wait time before next query due to: {{}}", this.workerType, this.workerID, e);
            LOGGER.error("", e);
        }
        super.waitTimeMs();
    }

    @Override
    public synchronized void addResults(QueryExecutionStats result) {
        this.results.add(result);
        this.executedQueries++;
    }

    @Override
    public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
        // If there is no more update send end signal, as there is nothing to do anymore
        if (this.queryCount >= this.queryHandler.getQueryCount()) {
            stopSending();
            return;
        }

        this.queryHandler.getNextQuery(queryStr, queryID);
        this.queryCount++;
    }

    /**
     * Sets Update Timer according to strategy
     *
     * @param strategyStr The String representation of a UpdateTimer.Strategy
     */
    private void setUpdateTimer(String strategyStr) {
        if (strategyStr == null) return;
        UpdateTimer.Strategy strategy = UpdateTimer.Strategy.valueOf(strategyStr.toUpperCase());
        switch (strategy) {
            case FIXED:
                if (this.timeLimit != null) {
                    this.updateTimer = new UpdateTimer(this.timeLimit / this.queryHandler.getQueryCount());
                } else {
                    LOGGER.warn("Worker[{{}} : {{}}]: FIXED Updates can only be used with timeLimit!", this.workerType, this.workerID);
                }
                break;
            case DISTRIBUTED:
                if (this.timeLimit != null) {
                    this.updateTimer = new UpdateTimer(this.queryHandler.getQueryCount(), this.timeLimit);
                } else {
                    LOGGER.warn("Worker[{{}} : {{}}]: DISTRIBUTED Updates can only be used with timeLimit!", this.workerType, this.workerID);
                }
                break;
            default:
                break;
        }
        LOGGER.debug("Worker[{{}} : {{}}]: UpdateTimer was set to UpdateTimer:{{}}", this.workerType, this.workerID, this.updateTimer);
    }


    /**
     * Checks if one queryMix was already executed, as it does not matter how many mixes should be executed
     *
     * @param noOfQueryMixes
     * @return
     */
    @Override
    public boolean hasExecutedNoOfQueryMixes(Long noOfQueryMixes) {
        return getExecutedQueries() / (getNoOfQueries() * 1.0) >= 1;
    }

}
