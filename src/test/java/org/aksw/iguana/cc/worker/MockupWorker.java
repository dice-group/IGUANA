package org.aksw.iguana.cc.worker;

import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.commons.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class MockupWorker extends AbstractWorker {

    private int counter = 0;
    private final String[] queries;

    public MockupWorker(String[] stringQueries, Integer workerID, @Nullable Integer timeLimit, ConnectionConfig connection, String taskID) {
        super(taskID, workerID, connection, getQueryConfig(), 0, timeLimit, 0, 0);
        this.queries = stringQueries;
    }

    public String[] getStringQueries() {
        return queries;
    }

    private static Map<String, Object> getQueryConfig() {
        Map<String, Object> queryConfig = new HashMap<>();
        queryConfig.put("location", "src/test/resources/mockupq.txt");
        return queryConfig;
    }

    @Override
    public void executeQuery(String query, String queryID) {
        long execTime = this.workerID * 10 + 100;
        long responseCode;
        long resultSize;
        try {
            Thread.sleep(execTime);
            responseCode = 200;
            resultSize = this.workerID * 100 + 100;
        } catch (InterruptedException e) {
            e.printStackTrace();
            responseCode = 400;
            resultSize = 0;
        }
        super.addResults(new QueryExecutionStats(queryID, responseCode, execTime, resultSize));
    }

    @Override
    public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) {
        if (this.counter >= this.queries.length) {
            this.counter = 0;
        }
        queryStr.append(this.queries[this.counter]);
        queryID.append("query:").append(this.counter);
        this.counter++;
    }
}
