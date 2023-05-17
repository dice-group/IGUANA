package org.aksw.iguana.cc.query.source.impl;

import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.cc.utils.IndexedQueryReader;
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

    private IndexedQueryReader iqr;

    /**
     * This constructor indexes the queries inside the given file. It assumes, that the queries inside the file are
     * separated with the default separator ('###').
     * @param path
     */
    public FileSeparatorQuerySource(String path) {
        this(path, DEFAULT_SEPARATOR);
    }

    /**
     * This constructor indexes the queries inside the given file. Queries inside the file should be separated with the
     * given separator string. If the separator string parameter is blank, it assumes that the queries inside the file
     * are separated by blank lines.
     * @param path
     * @param separator
     */
    public FileSeparatorQuerySource(String path, String separator) {
        super(path);

        try {
            if(separator.isBlank()) {
                iqr = IndexedQueryReader.makeWithBlankLines(path);
            }
            else {
                iqr = IndexedQueryReader.makeWithStringSeparator(path, separator);
            }
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
    public List<String> getAllQueries() throws IOException {
        return iqr.readQueries();
    }
}
