package org.aksw.iguana.cc.query.list.impl;

import org.aksw.iguana.cc.query.list.QueryList;
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
public class InMemQueryList extends QueryList {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemQueryList.class);

    private List<byte[]> queries; // TODO: make final

    public InMemQueryList(String name, QuerySource querySource) {
        super(name, querySource);
        try {
            this.queries = this.querySource.getAllQueries().stream().map(s->s.getBytes(StandardCharsets.UTF_8)).toList();
        } catch (IOException e) {
            LOGGER.error("Could not read queries");
        }
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
