package org.aksw.iguana.cc.query.list;

import org.aksw.iguana.cc.query.source.QuerySource;

import java.io.IOException;

/**
 * The abstract class for a QueryList. A query list provides the queries to the QueryHandler.
 *
 * @author frensing
 */
public abstract class QueryList {

    /** This is the QuerySource from which the queries should be retrieved. */
    protected QuerySource querySource;

    /** A name for the query list. This is a part of the queryIDs. */
    protected String name;

    public QueryList(String name, QuerySource querySource) {
        this.name = name;
        this.querySource = querySource;
    }

    /**
     * This method returns the amount of queries in the query list.
     *
     * @return The amount of queries in the query list
     */
    public int size() {
        return this.querySource.size();
    }

    /**
     * This method returns the name of the query list.
     *
     * @return The name of the query list
     */
    public String getName() {
        return this.name;
    }

    /**
     * This method returns the hashcode of the query list which is the hashcode of the query source.
     *
     * @return The hashcode of the query list
     */
    @Override
    public int hashCode() {
        return this.querySource.hashCode();
    }

    /**
     * This method returns a query at the given index.
     *
     * @param index Index of the query in the list
     * @return The query at the given index
     */
    public abstract String getQuery(int index) throws IOException;
}
