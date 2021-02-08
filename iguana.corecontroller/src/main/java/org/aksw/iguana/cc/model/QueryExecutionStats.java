package org.aksw.iguana.cc.model;

/**
 * Wrapper for a query execution.
 */
public class QueryExecutionStats {
    // TODO: queryID
    private String queryID;
    private long responseCode;
    private double executionTime;
    private long resultSize;

    public QueryExecutionStats(String queryID, long responseCode, double executionTime)
    {
        this.queryID = queryID;
        this.responseCode = responseCode;
        this.executionTime = executionTime;
    }


    public QueryExecutionStats(String queryID, long responseCode, double executionTime, long resultSize)
    {
        this.queryID = queryID;
        this.responseCode = responseCode;
        this.executionTime = executionTime;
        this.resultSize = resultSize;
    }

    public QueryExecutionStats() {
    }

    public String getQueryID() {
        return queryID;
    }

    public void setQueryID(String queryID) {
        this.queryID = queryID;
    }

    public long getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(long responseCode) {
        this.responseCode = responseCode;
    }

    public double getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(double executionTime) {
        this.executionTime = executionTime;
    }

    public long getResultSize() {
        return resultSize;
    }

    public void setResultSize(long resultSize) {
        this.resultSize = resultSize;
    }
}
