package org.aksw.iguana.cc.query.source;

import java.io.IOException;
import java.util.List;

/**
 * The QuerySource is providing the queries to the QuerySet.
 * It abstracts the actual format of the query files.
 *
 * @author frensing
 */
public interface QuerySource {

    /**
     * This method returns the number of queries in the source.
     *
     * @return the number of queries in the source
     */
    int size();

    /**
     * This method returns the query at the given index.
     *
     * @param index the index of the query counted from the first query (in the first file)
     * @return String of the query
     * @throws IOException
     */
    String getQuery(int index) throws IOException;

    /**
     * This method returns all queries in the source as Strings.
     *
     * @return List of Strings of all queries
     * @throws IOException
     */
    List<String> getAllQueries() throws IOException;

    /**
     * This method returns the hashcode of the source, calculated from the file contents rather than the Java object.
     *
     * @return the hashcode of the source
     */
    @Override
    int hashCode();
}
