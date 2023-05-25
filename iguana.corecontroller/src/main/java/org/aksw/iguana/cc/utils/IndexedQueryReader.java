package org.aksw.iguana.cc.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * This class creates objects, that index the start positions of lines inside a file for faster access. <br/>
 * It indexes either every single line or a bundle of multiple lines between two lines that contain a given separator.
 * <br/>
 * Blank content won't be indexed. <br/>
 * The positions and the length of the lines will be stored in an internal array.
 */
public class IndexedQueryReader {

    /** This list stores the start position and the length of the indexed lines inside the file. */
    private ArrayList<Long[]> indices;

    /** The file whose lines should be indexed. */
    private final File file;

    /** Number of indexed lines. */
    private int size;

    /**
     * Indexes every bundle of lines inside the file, that are in between two lines that contain the given separator
     * (including the beginning and end of the file). The given separator isn't allowed to be blank.
     * @param filepath path to the file
     * @param separator the separator line that is used in the file (isn't allowed to be blank)
     * @return reader to access the indexed lines
     * @throws IllegalArgumentException the given separator was blank
     * @throws IOException
     */
    public static IndexedQueryReader makeWithStringSeparator(String filepath, String separator) throws IOException {
        if(separator.isBlank())
            throw new IllegalArgumentException("Separator for makeWithStringSeparator can not be blank.");
        return new IndexedQueryReader(filepath, separator);
    }

    /**
     * Indexes every bundle of lines inside the file, that are in between two blank lines (including the beginning and
     * end of the file).
     * @param filepath path to the file
     * @return reader to access the indexed lines
     * @throws IOException
     */
    public static IndexedQueryReader makeWithEmptyLines(String filepath) throws IOException {
        String lineEnding = FileUtils.getLineEnding(filepath);
        return new IndexedQueryReader(filepath, lineEnding + lineEnding);
    }

    /**
     * Indexes every non-blank line inside the given file.
     * @param filepath path to the file
     * @return reader to access the indexed lines
     * @throws IOException
     */
    public static IndexedQueryReader make(String filepath) throws IOException {
        return new IndexedQueryReader(filepath, FileUtils.getLineEnding(filepath));
    }

    /**
     * Creates an object that indexes every bundle of multiple lines inside the given file, that are in between two
     * lines that contain the given separators (including the beginning and end of the given file). <br/>
     * If the separator is blank, every bundle between two blank lines will be indexed. <br/>
     * If the separator parameter is null, it will instead just index every non-blank line of the file.
     * @param filepath path to the file
     * @param separator the separator for each bundle
     * @throws IOException
     */
    private IndexedQueryReader(String filepath, String separator) throws IOException {
        this.file = new File(filepath);

        if(separator == null) {
            this.indexFile(FileUtils.getLineEnding(filepath));
        }
        else {
            this.indexFile(separator);
        }
    }

    /**
     * If a separator wasn't given, this method returns the line with the corresponding index inside the file. <br/>
     * If a separator was given, this method returns a string of multiple lines, that are between two separators
     * (including the beginning and end of file), with the corresponding index.
     * @param index the index of the searched line or bundle of lines
     * @return the searched line or bundle of lines
     * @throws IOException
     */
    public String readQuery(int index) throws IOException {
        // conversion from long to int (lines shouldn't be bigger than ~2GB)
        byte[] data = new byte[Math.toIntExact(this.indices.get(index)[1])];
        String output;
        try(RandomAccessFile raf = new RandomAccessFile(this.file, "r")) {
            raf.seek(this.indices.get(index)[0]);
            raf.read(data);
            output = new String(data, StandardCharsets.UTF_8);
        }
        return output;
    }

    /**
     * This method returns a list of strings that contains every indexed line or bundle of lines.
     * @return list of lines
     * @throws IOException
     */
    public List<String> readQueries() throws IOException {
        ArrayList<String> out = new ArrayList<>();
        for(int i = 0; i < indices.size(); i++) {
            out.add(this.readQuery(i));
        }
        return out;
    }

    /**
     * Returns the number of indexed non-blank lines or bundle of lines (depends on if a separator was given).
     * @return number of indexed objects
     */
    public int size() {
        return this.size;
    }

    /**
     * Indexes each bundle of lines in between two lines, that contain the given separator. If the content between two
     * separators is blank, this method won't index that bundle. If the separator is blank, each bundle between two
     * blank lines will be indexed. The beginning and end of file count as separators too for the indexing.
     * @param separator the custom separator
     * @throws IOException
     */
    private void indexFile(String separator) throws IOException {
        this.indices = new ArrayList<>();
        try(FileReader fr = new FileReader(this.file, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(fr)) {
            // The method needs to know the length of the line ending used in the file to be able to properly calculate
            // the starting byte position of a line
            long lastIndex = 0;
            long currentIndex = 0;
            int counter = 0;
            int character;
            boolean isWhiteSpace = true;
            while((character = br.read()) != -1) {
                if(character == (int) separator.toCharArray()[counter]) {
                    if(++counter >= separator.length()) {
                        if(!isWhiteSpace) {
                            this.indices.add(new Long[]{lastIndex, currentIndex - lastIndex});
                        }
                        currentIndex += counter;
                        counter = 0;
                        lastIndex = currentIndex;
                        isWhiteSpace = true;
                    }
                } else {
                    if(counter != 0) {
                        currentIndex += counter;
                        counter = 0;
                    }
                    if(isWhiteSpace && !Character.isWhitespace(character)) {
                        isWhiteSpace = false;
                    }
                    currentIndex++;
                }
            }
            if(!isWhiteSpace) {
                this.indices.add(new Long[]{lastIndex, currentIndex - lastIndex});
            }
        }
    }
}
