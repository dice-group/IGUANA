package org.aksw.iguana.cc.query.list.impl;

import org.aksw.iguana.cc.query.list.QueryList;
import org.aksw.iguana.cc.query.source.QuerySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * A query list which reads the queries into memory on initialization.
 * During the benchmark the query are returned from the memory.
 *
 * @author frensing
 */
public class InMemQueryList extends QueryList {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemQueryList.class);

    private List<String> queries;

    public InMemQueryList(String name, QuerySource querySource) {
        super(name, querySource);
        loadQueries();
    }

    private void loadQueries() {
        try {
            this.queries = this.querySource.getAllQueries();
        } catch (IOException e) {
            LOGGER.error("Could not read queries");
        }
    }

    @Override
    public String getQuery(int index) {
        return this.queries.get(index);
    }

    @Override
    public int size() {
        return this.queries.size();
    }
}
