package org.aksw.iguana.cc.query.list.impl;

import org.aksw.iguana.cc.query.list.FileBasedQueryList;
import org.aksw.iguana.cc.query.source.QuerySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A query list which reads the queries into memory on initialization.
 * During the benchmark the query are returned from the memory.
 *
 * @author frensing
 */
public class FileCachingQueryList extends FileBasedQueryList {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCachingQueryList.class);

    private final List<byte[]> queries;

    public FileCachingQueryList(QuerySource querySource) throws IOException {
        super(querySource);
        queries = this.querySource.getAllQueries().stream().map(s -> s.getBytes(StandardCharsets.UTF_8)).toList();
    }

    @Override
    public String getQuery(int index) {
        return new String(this.queries.get(index), StandardCharsets.UTF_8);
    }

    @Override
    public InputStream getQueryStream(int index) {
        return new ByteArrayInputStream(this.queries.get(index));
    }

    @Override
    public int size() {
        return this.queries.size();
    }
}
