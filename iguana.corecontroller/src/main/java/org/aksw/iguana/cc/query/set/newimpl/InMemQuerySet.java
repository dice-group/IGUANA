package org.aksw.iguana.cc.query.set.newimpl;

import org.aksw.iguana.cc.query.source.QuerySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class InMemQuerySet extends AbstractQuerySet {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemQuerySet.class);

    private List<String> queries;

    public InMemQuerySet(String name, QuerySource querySource) {
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
    public String getQueryAtPos(int pos) throws IOException {
        return this.queries.get(pos);
    }

    @Override
    public int size() {
        return this.queries.size();
    }
}
