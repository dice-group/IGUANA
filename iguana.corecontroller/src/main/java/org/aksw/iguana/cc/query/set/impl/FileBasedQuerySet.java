package org.aksw.iguana.cc.query.set.impl;

import org.aksw.iguana.cc.query.set.QuerySet;
import org.aksw.iguana.cc.query.source.QuerySource;

import java.io.IOException;

/**
 * A query set which reads the queries directly from a file.
 *
 * @author frensing
 */
public class FileBasedQuerySet extends QuerySet {

    public FileBasedQuerySet(String name, QuerySource querySource) {
        super(name, querySource);
    }

    @Override
    public String getQueryAtPos(int pos) throws IOException {
        return this.querySource.getQuery(pos);
    }
}
