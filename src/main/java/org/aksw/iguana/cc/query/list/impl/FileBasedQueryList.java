package org.aksw.iguana.cc.query.list.impl;

import org.aksw.iguana.cc.query.list.QueryList;
import org.aksw.iguana.cc.query.source.QuerySource;

import java.io.IOException;

/**
 * A query list which reads the queries directly from a file.
 *
 * @author frensing
 */
public class FileBasedQueryList extends QueryList {

    public FileBasedQueryList(String name, QuerySource querySource) {
        super(name, querySource);
    }

    @Override
    public String getQuery(int index) throws IOException {
        return this.querySource.getQuery(index);
    }
}
