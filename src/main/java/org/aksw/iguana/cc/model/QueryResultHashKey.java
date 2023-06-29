package org.aksw.iguana.cc.model;

import java.util.Objects;

/**
 * Creates a Result Hash key for a query, thus a result size only has to be checked once and it will be cached using this key
 */
public class QueryResultHashKey {

    private String queryId;
    private long uniqueKey;

    public QueryResultHashKey(String queryId, long uniqueKey) {
        this.queryId = queryId;
        this.uniqueKey = uniqueKey;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public long getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(long uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryResultHashKey that = (QueryResultHashKey) o;
        return uniqueKey == that.uniqueKey &&
                queryId.equals(that.queryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryId, uniqueKey);
    }

    @Override
    public String toString() {
        return "QueryResultHashKey{" +
                "queryId='" + queryId + '\'' +
                ", uniqueKey=" + uniqueKey +
                '}';
    }
}
