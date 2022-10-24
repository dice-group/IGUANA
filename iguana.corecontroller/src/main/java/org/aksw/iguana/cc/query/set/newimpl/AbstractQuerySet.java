package org.aksw.iguana.cc.query.set.newimpl;

import org.aksw.iguana.cc.query.set.QuerySet;
import org.aksw.iguana.cc.query.source.QuerySource;

public abstract class AbstractQuerySet implements QuerySet {
    protected QuerySource querySource;

    protected String name;

    public AbstractQuerySet(String name, QuerySource querySource) {
        this.name = name;
        this.querySource = querySource;
    }

    @Override
    public int size() {
        return this.querySource.size();
    }

    @Override
    public String getName() {
        return this.name;
    }
}