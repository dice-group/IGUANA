package org.aksw.iguana.tp.model;

public class QueryExecutionStats {

    private long responseCode;
    private double executionTime;
    private long resultSize;

    public QueryExecutionStats(long responseCode, double executionTime)
    {
        this.responseCode = responseCode;
        this.executionTime = executionTime;
    }


    public QueryExecutionStats(long responseCode, double executionTime, long resultSize)
    {
        this.responseCode = responseCode;
        this.executionTime = executionTime;
        this.resultSize = resultSize;
    }

    public QueryExecutionStats() {

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
