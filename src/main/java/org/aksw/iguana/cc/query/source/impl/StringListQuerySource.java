package org.aksw.iguana.cc.query.source.impl;

import org.aksw.iguana.cc.query.source.QuerySource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * A query source which reads the queries from a list of strings.
 */
public class StringListQuerySource extends QuerySource {

    private final List<String> queries;

    public StringListQuerySource(List<String> queries) {
        super(null);
        this.queries = queries;
    }

    @Override
    public int size() {
        return queries.size();
    }

    @Override
    public String getQuery(int index) throws IOException {
        return queries.get(index);
    }

    @Override
    public InputStream getQueryStream(int index) throws IOException {
        return new ByteArrayInputStream(queries.get(index).getBytes());
    }

    /**
     * Returns all queries in the source as a list of Strings.
     * The list is unmodifiable.
     *
     * @return List of Strings of all queries
     * @throws IOException won't be thrown
     */
    @Override
    public List<String> getAllQueries() throws IOException {
        return Collections.unmodifiableList(queries);
    }
}
