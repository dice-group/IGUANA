package org.aksw.iguana.cc.query.list;

import org.aksw.iguana.cc.query.source.QuerySource;

import java.io.IOException;
import java.io.InputStream;

/**
 * The abstract class for a QueryList. A query list provides the queries to the QueryHandler.
 *
 * @author frensing
 */
public abstract class FileBasedQueryList implements QueryList{

    /**
     * This is the QuerySource from which the queries should be retrieved.
     */
    final protected QuerySource querySource;

    public FileBasedQueryList(QuerySource querySource) {
        if (querySource == null) {
            throw new IllegalArgumentException("QuerySource must not be null");
        }
        this.querySource = querySource;
    }

    /**
     * This method returns the amount of queries in the query list.
     *
     * @return The amount of queries in the query list
     */
    public int size() {
        return querySource.size();
    }

    /**
     * This method returns the hashcode of the query list which is the hashcode of the query source.
     *
     * @return The hashcode of the query list
     */
    @Override
    public int hashCode() {
        return querySource.hashCode();
    }

    /**
     * This method returns a query at the given index.
     *
     * @param index Index of the query in the list
     * @return The query at the given index
     */
    public abstract String getQuery(int index) throws IOException;

    public abstract InputStream getQueryStream(int index) throws IOException;
}
