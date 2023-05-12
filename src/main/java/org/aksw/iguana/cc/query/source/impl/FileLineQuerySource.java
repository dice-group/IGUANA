package org.aksw.iguana.cc.query.source.impl;

import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.cc.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * The FileLineQuerySource reads queries from a file with one query per line.
 *
 * @author frensing
 */
public class FileLineQuerySource extends QuerySource {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileLineQuerySource.class);

    protected File queryFile;

    protected int size;

    public FileLineQuerySource(String path) {
        super(path);
        this.queryFile = new File(this.path);
        try {
            this.size = FileUtils.countLines(this.queryFile);
        } catch (IOException e) {
            LOGGER.error("Could not read queries");
        }
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public String getQuery(int index) throws IOException {
        return FileUtils.readLineAt(index, this.queryFile);
    }

    @Override
    public List<String> getAllQueries() throws IOException {
        return Files.readAllLines(this.queryFile.toPath());
    }
}
