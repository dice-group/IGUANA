package org.aksw.iguana.cc.query;

import org.aksw.iguana.cc.query.source.QuerySource;
import org.apache.jena.update.UpdateFactory;

import java.io.ByteArrayInputStream;
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
 * @param update  If the query is an update query
 */
public record QueryData(int queryId, boolean update) {
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
            queryData.add(new QueryData(i++, update));
        }
        return queryData;
    }

    public static List<QueryData> generate(QuerySource queries) throws IOException {
        final var streams = new ArrayList<InputStream>();
        int bound = queries.size();
        for (int i = 0; i < bound; i++) {
            InputStream queryStream = queries.getQueryStream(i);
            streams.add(queryStream);
        }
        return generate(streams);
    }

    public static List<QueryData> generate(List<String> queries) {
        final var streams = queries.stream().map(s -> (InputStream) new ByteArrayInputStream(s.getBytes())).toList();
        return generate(streams);
    }
}
