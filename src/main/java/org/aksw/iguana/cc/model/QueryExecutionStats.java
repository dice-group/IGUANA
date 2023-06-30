package org.aksw.iguana.cc.model;

/**
 * Wrapper for a query execution.
 */
public record QueryExecutionStats (
    String queryID,
    long responseCode,
    double executionTime,
    long resultSize
) {
    public QueryExecutionStats(String queryID, long responseCode, double executionTime) {
        this(queryID, responseCode, executionTime, 0);
    }
}
