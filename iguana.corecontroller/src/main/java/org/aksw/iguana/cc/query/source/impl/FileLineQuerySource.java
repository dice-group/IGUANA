package org.aksw.iguana.cc.query.source.impl;

import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.cc.utils.IndexedLineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * The FileLineQuerySource reads queries from a file with one query per line.
 *
 * @author frensing
 */
public class FileLineQuerySource extends QuerySource {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileLineQuerySource.class);

    private IndexedLineReader ilr;

    public FileLineQuerySource(String path) {
        super(path);

        try {
            ilr = new IndexedLineReader(path);
        } catch (IOException e) {
            LOGGER.error("Failed to read this file for the queries: " + path + "\n" + e);
        }
    }

    @Override
    public int size() {
        return ilr.size();
    }

    @Override
    public String getQuery(int index) throws IOException {
        return ilr.readLine(index);
    }

    @Override
    public List<String> getAllQueries() throws IOException {
        return ilr.readLines();
    }
}
