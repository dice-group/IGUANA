package org.aksw.iguana.cc.query.source.impl;

import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.cc.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The FileSeparatorQuerySource reads queries from a folder with query files.
 * Each query contains one (multiline) query.
 *
 * @author frensing
 */
public class FolderQuerySource extends QuerySource {

    protected static final Logger LOGGER = LoggerFactory.getLogger(FolderQuerySource.class);

    protected File[] files;

    public FolderQuerySource(String path) {
        super(path);

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
        this.files = dir.listFiles(File::isFile);
        if (this.files == null)
            this.files = new File[]{};
        Arrays.sort(this.files);
    }

    @Override
    public int size() {
        return this.files.length;
    }

    @Override
    public String getQuery(int index) throws IOException {
        return FileUtils.readFile(files[index].getAbsolutePath());
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
        return FileUtils.getHashcodeFromFileContent(this.files[0].getAbsolutePath());
    }
}
