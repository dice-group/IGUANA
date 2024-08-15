package org.aksw.iguana.cc.query.list.impl;

import org.aksw.iguana.cc.query.list.QueryList;
import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.commons.io.BigByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A query list which reads the queries into memory on initialization.
 * During the benchmark the query are returned from the memory.
 *
 * @author frensing
 */
public class InMemQueryList extends QueryList {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemQueryList.class);

    private final List<BigByteArrayOutputStream> queries = new ArrayList<>();

    public InMemQueryList(QuerySource querySource) throws IOException {
        super(querySource);
        for (int i = 0; i < querySource.size(); i++) {
            try (InputStream queryStream = querySource.getQueryStream(i)) {
                BigByteArrayOutputStream baos = new BigByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int len;
                while ((len = queryStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                baos.close();
                queries.add(baos);
            }
        }
    }

    @Override
    public String getQuery(int index) {
        final var queryStream = queries.get(index);
        if (queryStream.size() > Integer.MAX_VALUE - 8) {
            throw new OutOfMemoryError("Query is too large to be read into a string object.");
        }
        return new String(queryStream.toByteArray()[0], StandardCharsets.UTF_8);
    }

    @Override
    public InputStream getQueryStream(int index) {
        final var stream = BigByteArrayOutputStream.shallowCopy(queries.get(index));
        return stream.toInputStream();
    }

    @Override
    public int size() {
        return this.queries.size();
    }
}
