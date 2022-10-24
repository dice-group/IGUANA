package org.aksw.iguana.cc.query.source;

import org.aksw.iguana.cc.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class FileLineQuerySource implements QuerySource {

    protected String path;
    protected File queryFile;

    protected int size;

    public FileLineQuerySource(String path) throws IOException {
        this.path = path;
        this.queryFile = new File(this.path);
        this.size = FileUtils.countLines(this.queryFile);
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
