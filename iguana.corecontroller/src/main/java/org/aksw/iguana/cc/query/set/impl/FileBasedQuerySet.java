package org.aksw.iguana.cc.query.set.impl;

import org.aksw.iguana.cc.query.set.AbstractQuerySet;
import org.aksw.iguana.cc.query.source.QuerySource;

import java.io.IOException;

public class FileBasedQuerySet extends AbstractQuerySet {

    public FileBasedQuerySet(String name, QuerySource querySource) {
        super(name, querySource);
    }

    @Override
    public String getQueryAtPos(int pos) throws IOException {
        return this.querySource.getQuery(pos);
    }
}
