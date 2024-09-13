package org.aksw.iguana.cc.query.list.impl;

import org.aksw.iguana.cc.query.list.FileBasedQueryList;
import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.commons.io.ByteArrayListInputStream;
import org.aksw.iguana.commons.io.ByteArrayListOutputStream;
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
public class FileCachingQueryList extends FileBasedQueryList {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCachingQueryList.class);

    private final List<ByteArrayListOutputStream> queries = new ArrayList<>();

    public FileCachingQueryList(QuerySource querySource) throws IOException {
        super(querySource);
        LOGGER.info("Reading queries from the source with the hash code {} into memory.", querySource.hashCode());
        for (int i = 0; i < querySource.size(); i++) {
            try (InputStream queryStream = querySource.getQueryStream(i)) {
                ByteArrayListOutputStream balos = new ByteArrayListOutputStream();
                byte[] currentBuffer;
                do {
                    currentBuffer = queryStream.readNBytes(Integer.MAX_VALUE / 2);
                    balos.write(currentBuffer);
                } while (currentBuffer.length == Integer.MAX_VALUE / 2);
                balos.close();
                queries.add(balos);
            }
        }
    }

    @Override
    public String getQuery(int index) {
        final var queryStream = queries.get(index);
        if (queryStream.size() > Integer.MAX_VALUE - 8) {
            throw new OutOfMemoryError("Query is too large to be read into a string object.");
        }

        byte[] buffer;
        try {
            buffer = queryStream.toInputStream().readNBytes(Integer.MAX_VALUE - 8);
        } catch (IOException ignored) {
            LOGGER.error("Could not read query into string.");
            return "";
        }
        return new String(buffer, StandardCharsets.UTF_8);
    }

    @Override
    public InputStream getQueryStream(int index) {
        return new ByteArrayListInputStream(queries.get(index).getBuffers());
    }

    @Override
    public int size() {
        return this.queries.size();
    }
}
