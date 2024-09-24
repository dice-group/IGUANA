package org.aksw.iguana.cc.query.list.impl;

import org.aksw.iguana.cc.query.QueryData;
import org.aksw.iguana.cc.query.list.FileBasedQueryList;
import org.aksw.iguana.cc.query.source.QuerySource;

import java.io.IOException;
import java.io.InputStream;

/**
 * A query list which reads the queries directly from a file.
 *
 * @author frensing
 */
public class FileReadingQueryList extends FileBasedQueryList {

    public FileReadingQueryList(QuerySource querySource) {
        super(querySource);
    }

    @Override
    public String getQuery(int index) throws IOException {
        return querySource.getQuery(index);
    }

    @Override
    public InputStream getQueryStream(int index) throws IOException {
        return querySource.getQueryStream(index);
    }
}
