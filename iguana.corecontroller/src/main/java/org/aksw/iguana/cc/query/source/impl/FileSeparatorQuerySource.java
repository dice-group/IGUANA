package org.aksw.iguana.cc.query.source.impl;

import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.cc.utils.IndexedLineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * The FileSeparatorQuerySource reads queries from a file with
 * (multiline) queries that are separated by a separator line.
 *
 * @author frensing
 */
public class FileSeparatorQuerySource extends QuerySource {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSeparatorQuerySource.class);

    private static final String DEFAULT_SEPARATOR = "###";

    private IndexedLineReader ilr;

    public FileSeparatorQuerySource(String path) {
        this(path, DEFAULT_SEPARATOR);
    }

    public FileSeparatorQuerySource(String path, String separator) {
        super(path);

        try {
            if(separator.isBlank()) {
                ilr = IndexedLineReader.makeWithBlankLines(path);
            }
            else {
                ilr = IndexedLineReader.makeWithStringSeparator(path, separator);
            }
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
