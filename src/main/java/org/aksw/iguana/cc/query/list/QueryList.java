package org.aksw.iguana.cc.query.list;

import org.aksw.iguana.cc.query.QueryData;
import org.aksw.iguana.cc.query.source.QuerySource;

import java.io.IOException;
import java.io.InputStream;

/**
 * The abstract class for a QueryList. A query list provides the queries to the QueryHandler.
 *
 * @author frensing
 */
public interface QueryList {

    /**
     * This method returns the amount of queries in the query list.
     *
     * @return The amount of queries in the query list
     */
    int size();

    /**
     * This method returns the hashcode of the query list which is the hashcode of the query source.
     *
     * @return The hashcode of the query list
     */
    int hashCode();

    /**
     * This method returns a query at the given index.
     *
     * @param index Index of the query in the list
     * @return The query at the given index
     */
    String getQuery(int index) throws IOException;

    InputStream getQueryStream(int index) throws IOException;

    QueryData getQueryData(int index);
}
