package org.aksw.iguana.cc.query.source.impl;

import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.cc.utils.files.FileUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.text.MessageFormat.format;

/**
 * The FileSeparatorQuerySource reads queries from a folder with query files.
 * Each query contains one (multiline) query.
 *
 * @author frensing
 */
public class FolderQuerySource extends QuerySource {

    protected static final Logger LOGGER = LoggerFactory.getLogger(FolderQuerySource.class);

    protected Path[] files;

    public FolderQuerySource(Path path) throws IOException {
        super(path);

        if (!Files.isDirectory(path)) {
            final var message = format("Folder does not exist {0}.", path);
            LOGGER.error(message);
            throw new IOException(message);
        }

        LOGGER.info("Indexing folder {}.", path);

        try (Stream<Path> pathStream = Files.list(path);) {
            files = pathStream
                    .filter(p -> Files.isReadable(p) && Files.isRegularFile(p))
                    .sorted()
                    .toArray(Path[]::new);
        }

    }

    @Override
    public int size() {
        return this.files.length;
    }

    @Override
    public String getQuery(int index) throws IOException {
        return Files.readString(files[index], StandardCharsets.UTF_8);
    }

    @Override
    public InputStream getQueryStream(int index) throws IOException {
        return new AutoCloseInputStream(new BufferedInputStream(new FileInputStream(files[index].toFile())));
    }

    @Override
    public List<String> getAllQueries() throws IOException {
        List<String> queries = new ArrayList<>(this.files.length);
        for (int i = 0; i < this.files.length; i++) {
            queries.add(getQuery(i));
        }
        return queries;
    }

    @Override
    public int hashCode() {
        return FileUtils.getHashcodeFromDirectory(this.path, true);
    }
}
