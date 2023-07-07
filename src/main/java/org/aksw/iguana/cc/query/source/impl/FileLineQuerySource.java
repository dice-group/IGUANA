package org.aksw.iguana.cc.query.source.impl;

import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.cc.utils.IndexedQueryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * The FileLineQuerySource reads queries from a file with one query per line.
 *
 * @author frensing
 */
public class FileLineQuerySource extends QuerySource {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileLineQuerySource.class);

    private IndexedQueryReader iqr;

    public FileLineQuerySource(Path path) {
        super(path);

        try {
            iqr = IndexedQueryReader.make(path);
        } catch (IOException e) {
            LOGGER.error("Failed to read this file for the queries: " + path + "\n" + e);
        }
    }

    @Override
    public int size() {
        return iqr.size();
    }

    @Override
    public String getQuery(int index) throws IOException {
        return iqr.readQuery(index);
    }

    @Override
    public InputStream getQueryStream(int index) throws IOException {
        return iqr.streamQuery(index);
    }

    @Override
    public List<String> getAllQueries() throws IOException {
        return iqr.readQueries();
    }
}
