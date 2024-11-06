package org.aksw.iguana.cc.query;

import org.apache.jena.update.UpdateFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class stores extra information about a query.
 * At the moment, it only stores if the query is an update query or not.
 *
 * @param queryId The id of the query
 */
public record QueryData(int queryId, QueryType type, Integer templateId) {
    public enum QueryType {
        DEFAULT,
        UPDATE,
        TEMPLATE,
        TEMPLATE_INSTANCE
    }

    /**
     * Generates a list of QueryData objects for a collection of queries.
     * The method uses the Jena library to check if the query is an update query.
     * It only checks if the query is an update query or not and sets their index in the order they were given.
     *
     * @param queries collection of input streams of queries
     * @return list of QueryData objects
     */
    public static List<QueryData> generate(Collection<InputStream> queries) {
        final var queryData = new ArrayList<QueryData>();
        int i = 0;
        for (InputStream query : queries) {
            boolean update = true;
            try {
                UpdateFactory.read(query); // Throws an exception if the query is not an update query
            } catch (Exception e) {
                update = false;
            }
            queryData.add(new QueryData(i++, update ? QueryType.UPDATE : QueryType.DEFAULT, null));
            try {
                query.close();
            } catch (IOException ignored) {}
        }
        return queryData;
    }

    /**
     * Checks if the given query is an update query.
     * The method uses the Jena library to check if the query is an update query.
     *
     * @param query input stream of the query
     * @return true if the query is an update query, false otherwise
     */
    public static boolean checkIfUpdate(InputStream query) {
        try {
            UpdateFactory.read(query); // Throws an exception if the query is not an update query
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean update() {
        return type == QueryType.UPDATE;
    }
}
