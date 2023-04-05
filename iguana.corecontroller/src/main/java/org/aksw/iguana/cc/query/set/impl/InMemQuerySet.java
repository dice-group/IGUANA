package org.aksw.iguana.cc.query.set.impl;

import org.aksw.iguana.cc.query.set.AbstractQuerySet;
import org.aksw.iguana.cc.query.source.AbstractQuerySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * A query set which reads the queries into memory on initialization.
 * During the benchmark the query are returned from the memory.
 *
 * @author frensing
 */
public class InMemQuerySet extends AbstractQuerySet {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemQuerySet.class);

    private List<String> queries;

    public InMemQuerySet(String name, AbstractQuerySource querySource) {
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
    public String getQueryAtPos(int pos) {
        return this.queries.get(pos);
    }

    @Override
    public int size() {
        return this.queries.size();
    }
}
