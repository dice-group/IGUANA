package org.aksw.iguana.cc.query.set;

import org.aksw.iguana.cc.query.source.QuerySource;

/**
 * The abstract class for a QuerySet.
 * It implements the basic functions that are shared for all QuerySets.
 *
 * @author frensing
 */
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

    @Override
    public int hashCode() {
        return this.querySource.hashCode();
    }
}
