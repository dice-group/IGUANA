package org.aksw.iguana.cc.worker;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.commons.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class MockupWorker extends AbstractWorker {

    private int counter = 0;
    private final String[] queries;

    public MockupWorker(String[] stringQueries, Integer workerID, @Nullable Integer timeLimit, Connection connection, String taskID) {
        super(taskID, workerID, connection, getQueryConfig(), 0, timeLimit, 0, 0);
        this.queries = stringQueries;
    }

    public String[] getStringQueries() {
        return queries;
    }

    private static Map<Object, Object> getQueryConfig() {
        Map<Object, Object> queryConfig = new HashMap<>();
        queryConfig.put("location", "src/test/resources/mockupq.txt");
        return queryConfig;
    }

    @Override
    public void executeQuery(String query, String queryID) {
        QueryExecutionStats results = new QueryExecutionStats();
        long execTime = this.workerID * 10 + 100;
        try {
            Thread.sleep(execTime);
            results.setResponseCode(200);
            results.setResultSize(this.workerID * 100 + 100);
        } catch (InterruptedException e) {
            e.printStackTrace();
            results.setResponseCode(400);
            results.setResultSize(0);
        }
        results.setExecutionTime(execTime);
        results.setQueryID(queryID);
        super.addResults(results);
    }

    @Override
    public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) {
        if (this.counter >= this.queries.length) {
            this.counter = 0;
        }
        queryStr.append(this.queries[this.counter]);
        queryID.append("query").append(this.counter);
        this.counter++;
    }
}
