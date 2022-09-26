package org.aksw.iguana.cc.worker;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.commons.annotation.Nullable;

import java.io.IOException;

public class MockupWorker extends AbstractWorker {

    private int counter = 0;
    private String[] queries = new String[]{

    };

    public MockupWorker(String[] queries, Integer workerID, @Nullable Integer timeLimit, Connection connection, String taskID) {
        super(taskID, connection, "src/test/resources/mockupq.txt", 0, timeLimit, 0, 0, workerID);
        this.queries = queries;
    }


    public MockupWorker(String taskID, Connection connection, String queriesFile, Integer timeOut, Integer timeLimit, Integer fixedLatency, Integer gaussianLatency, Integer workerID) {
        super(taskID, connection, queriesFile, timeOut, timeLimit, fixedLatency, gaussianLatency, workerID);
    }

    public String[] getStringQueries() {
        return queries;
    }

    @Override
    public void executeQuery(String query, String queryID) {
        QueryExecutionStats results = new QueryExecutionStats();
        long execTime = workerID * 10 + 100;
        try {
            Thread.sleep(execTime);
            results.setResponseCode(200);
            results.setResultSize(workerID*100+100);
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
    public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
        if(counter>=queries.length){
            counter=0;
        }
        queryStr.append(queries[counter]);
        queryID.append("query").append(counter);
        counter++;
    }
}
