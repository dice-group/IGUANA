package org.aksw.iguana.cc.query.set;


import java.io.IOException;

/**
 * A query set provides the Queries to the QueryHandler.
 */
public interface QuerySet {

    /**
     * Gets a query at the position pos.
     *
     * @param pos Position of the query in the set
     * @return The query at position pos
     */
    String getQueryAtPos(int pos) throws IOException;

    /**
     * Gets no. of queries in the query set
     *
     * @return The no of queries in the query set
     */
    int size();

    /**
     * Gets the name of the query set
     *
     * @return The name of the query set
     */
    String getName();

    /**
     * Gets the hashcode of the query set which is the hashcode of the query source
     *
     * @return The hashcode of the query set
     */
    @Override
    int hashCode();
}
