package org.aksw.iguana.cc.query.source.impl;

import org.aksw.iguana.cc.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * The FileLineQuerySource reads queries from a file with one query per line.
 *
 * @author frensing
 */
public class FileLineQuerySource extends FileSeparatorQuerySource {
    public FileLineQuerySource(Path filepath) throws IOException {
        super(filepath, FileUtils.getLineEnding(filepath));
    }

}
