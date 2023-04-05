package org.aksw.iguana.cc.query.set;

import org.aksw.iguana.cc.query.source.AbstractQuerySource;

import java.io.IOException;

/**
 * The abstract class for a QuerySet. A query set provides the queries to the QueryHandler.
 *
 * @author frensing
 */
public abstract class AbstractQuerySet {

    /** This is the QuerySource from which the queries should be retrieved. */
    protected AbstractQuerySource querySource;

    /** A name for the query set. This is a part of the queryIDs. */
    protected String name;

    public AbstractQuerySet(String name, AbstractQuerySource querySource) {
        this.name = name;
        this.querySource = querySource;
    }

    /**
     * This method returns the amount of queries in the query set.
     *
     * @return The amount of queries in the query set
     */
    public int size() {
        return this.querySource.size();
    }

    /**
     * This method returns the name of the query set.
     *
     * @return The name of the query set
     */
    public String getName() {
        return this.name;
    }

    /**
     * This method returns the hashcode of the query set which is the hashcode of the query source.
     *
     * @return The hashcode of the query set
     */
    @Override
    public int hashCode() {
        return this.querySource.hashCode();
    }

    /**
     * This method returns a query at the given position.
     *
     * @param pos Position of the query in the set
     * @return The query at position pos
     */
    public abstract String getQueryAtPos(int pos) throws IOException;
}
