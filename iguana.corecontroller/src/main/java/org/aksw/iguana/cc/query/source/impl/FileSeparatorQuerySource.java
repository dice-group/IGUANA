package org.aksw.iguana.cc.query.source.impl;

import org.aksw.iguana.cc.query.source.AbstractQuerySource;
import org.aksw.iguana.cc.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class FileSeparatorQuerySource extends AbstractQuerySource {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSeparatorQuerySource.class);

    private static final String DEFAULT_SEPARATOR = "###";

    protected File queryFile;
    protected String separator;
    protected int size;

    private List<Integer> separatorPositions;

    public FileSeparatorQuerySource(String path) {
        this(path, DEFAULT_SEPARATOR);
    }

    public FileSeparatorQuerySource(String path, String separator) {
        super(path);
        this.queryFile = new File(this.path);
        this.separator = separator;

        indexFile();
    }

    private void indexFile() {
        this.separatorPositions = new LinkedList<>();
        int separatorCount = 0;
        try (BufferedReader reader = FileUtils.getBufferedReader(this.queryFile)) {
            int index = 0;
            String line;
            this.separatorPositions.add(-1);
            while ((line = reader.readLine()) != null) {
                if (line.equals(this.separator)) {
                    separatorCount++;
                    this.separatorPositions.add(index);
                }
                index++;
            }
            this.separatorPositions.add(index);
        } catch (IOException e) {
            LOGGER.error("Could not read queries");
        }

        this.size = separatorCount + 1;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public String getQuery(int index) throws IOException {
        int start = this.separatorPositions.get(index) + 1;
        int end = this.separatorPositions.get(index + 1);

        try (Stream<String> lines = Files.lines(this.queryFile.toPath())) {
            return lines.skip(start)
                    .limit(end - start)
                    .reduce((a, b) -> a + b)
                    .get();
        } catch (FileNotFoundException e) {
            LOGGER.error("Could not read queries");
        }
        return null;
    }

    @Override
    public List<String> getAllQueries() throws IOException {
        try (BufferedReader reader = FileUtils.getBufferedReader(this.queryFile)) {
            List<String> queries = new ArrayList<>(this.size);
            String line;
            StringBuilder query = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.equals(this.separator)) {
                    queries.add(query.toString());
                    query = new StringBuilder();
                } else {
                    query.append(line);
                }
            }
            queries.add(query.toString());
            return queries;
        }
    }

}
