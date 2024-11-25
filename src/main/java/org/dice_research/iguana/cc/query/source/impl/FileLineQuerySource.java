package org.dice_research.iguana.cc.query.source.impl;

import org.dice_research.iguana.cc.utils.files.FileUtils;

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
