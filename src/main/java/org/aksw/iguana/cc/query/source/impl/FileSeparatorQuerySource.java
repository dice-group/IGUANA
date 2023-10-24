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
 * The FileSeparatorQuerySource reads queries from a file with
 * (multiline) queries that are separated by a separator.
 *
 * @author frensing
 */
public class FileSeparatorQuerySource extends QuerySource {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSeparatorQuerySource.class);

    private static final String DEFAULT_SEPARATOR = "###";

    final protected IndexedQueryReader iqr;

    /**
     * This constructor indexes the queries inside the given file. It assumes, that the queries inside the file are
     * separated with the default separator ('###').
     *
     * @param path path to the queries-file
     */
    public FileSeparatorQuerySource(Path path) throws IOException {
        super(path);
        iqr = getIqr(path, DEFAULT_SEPARATOR);
    }

    /**
     * This constructor indexes the queries inside the given file. Queries inside the file should be separated with the
     * given separator string. If the separator string parameter is blank, it assumes that the queries inside the file
     * are separated by blank lines.
     *
     * @param path path to the queries-file
     * @param separator string with which the queries inside the file are separated
     */
    public FileSeparatorQuerySource(Path path, String separator) throws IOException {
        super(path);
        iqr = getIqr(path, separator);

    }

    private static IndexedQueryReader getIqr(Path path, String separator) throws IOException {
        return (separator.isEmpty()) ? IndexedQueryReader.makeWithEmptyLines(path) : IndexedQueryReader.makeWithStringSeparator(path, separator);
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
