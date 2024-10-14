package org.aksw.iguana.cc.query.list.impl;

import org.aksw.iguana.cc.query.list.QueryList;
import org.aksw.iguana.commons.io.ByteArrayListInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class StringListQueryList implements QueryList {

    private final List<String> queries;

    public StringListQueryList(List<String> queries) {
        this.queries = queries;
    }

    @Override
    public String getQuery(int index) throws IOException {
        return queries.get(index);
    }

    @Override
    public InputStream getQueryStream(int index) throws IOException {
        return new ByteArrayListInputStream(List.of(queries.get(index).getBytes()));
    }

    @Override
    public int size() {
        return queries.size();
    }

    @Override
    public int hashCode() {
        return queries.hashCode();
    }
}
