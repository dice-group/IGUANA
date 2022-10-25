package org.aksw.iguana.cc.query.set;


import java.io.IOException;

/**
 * A query set contains a benchmark query (this might be several queries in itself)
 */
public interface QuerySet {

    /**
     * Gets a query at the position pos.
     *
     * @param pos Position of the query in the set
     * @return The query at position pos
     */
    String getQueryAtPos(int pos) throws IOException;

    int size();

    String getName();

    int getHashcode();
}
