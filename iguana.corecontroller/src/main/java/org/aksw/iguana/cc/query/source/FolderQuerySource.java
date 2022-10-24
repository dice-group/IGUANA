package org.aksw.iguana.cc.query.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FolderQuerySource implements QuerySource {

    protected static final Logger LOGGER = LoggerFactory.getLogger(FolderQuerySource.class);

    protected String path;
    protected File[] files;

    public FolderQuerySource(String path) {
        this.path = path;

        indexFolder();
    }

    private void indexFolder() {
        File dir = new File(this.path);
        if (!dir.exists()) {
            LOGGER.error("Folder does not exist");
            return;
        }
        if (!dir.isDirectory()) {
            LOGGER.error("Path is not a folder");
            return;
        }

        LOGGER.info("indexing folder {}", this.path);
        this.files = dir.listFiles();
    }

    @Override
    public int size() {
        return this.files.length;
    }

    @Override
    public String getQuery(int index) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(this.files[index]))) {
            StringBuilder query = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                query.append(line);
            }
            return query.toString();
        }
    }

    @Override
    public List<String> getAllQueries() throws IOException {
        List<String> queries = new ArrayList<>(this.files.length);
        for (int i = 0; i < this.files.length; i++) {
            queries.add(getQuery(i));
        }
        return queries;
    }
}
