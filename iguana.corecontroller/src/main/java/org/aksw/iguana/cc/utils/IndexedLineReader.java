package org.aksw.iguana.cc.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This class creates an object, that indexes the starting positions of the lines inside a file for faster access. <br/>
 * The indexing happens on either:
 * <ul>
 *     <li>the file's line endings, or</li>
 *     <li>a custom line separator</li>
 * </ul>
 * This class does not index blank lines.
 */
public class IndexedLineReader {

    /** This list stores the indices of the bytes, at which a new line starts. (Stores Integers, so it only supports
     *  files that are roughly 2GB big.)*/
    private ArrayList<Integer> indices;

    private String filepath;

    /** Stores the filesize in number of bytes. */
    private int filesize;

    /** If no line separator is given through the constructor, this string stays empty. */
    private String separator = "";

    private int size;

    public IndexedLineReader(String filepath) throws IOException {
        this.filepath = filepath;
        this.filesize = (int) Files.size(Paths.get(filepath));
        this.indexFile();
    }

    public IndexedLineReader(String filepath, String separator) throws IOException {
        this.filepath = filepath;
        this.filesize = (int) Files.size(Paths.get(filepath));
        this.separator = separator;
        this.indexFile(separator);
    }

    /**
     * This method reads a line from the file at a given index. It will skip every byte of data, that is written in the
     * file before the line. <br/>
     * If the lines were indexed based on a custom line separator, this method returns every character between two of
     * the given line separators (the beginning and ending of the file count as line separators too).
     * @param index the index of the line
     * @return the searched line
     * @throws IOException
     */
    public String readLine(int index) throws IOException {
        if(!separator.isEmpty()) {
            // If a custom line separator has been used, the method readLine from RandomAccessFile can't be used, thus
            // a different implementation is needed
            return readLinesBetweenSeparator(index);
        }

        RandomAccessFile raf = new RandomAccessFile(this.filepath, "r");
        raf.seek(this.indices.get(index));
        String output = raf.readLine();
        raf.close();
        return output;
    }

    /**
     * This method return a list that contains every line of the file.
     * @return list of lines
     * @throws IOException
     */
    public List<String> readLines() throws IOException {
        ArrayList<String> out = new ArrayList<>();
        for(int i = 0; i < indices.size(); i++) {
            out.add(this.readLine(i));
        }
        return out;
    }

    /**
     * Returns the number of non-blank lines the file contains.
     * @return number of non-blank lines
     */
    public int size() {
        return this.size;
    }

    /**
     * This method reads the bytes of data between the given index and the next index (or file end), parses it to a
     * string and removes every character that appears after a line separator and the line separator as well.
     * @param index
     * @return the string between two line separators
     * @throws IOException
     */
    private String readLinesBetweenSeparator(int index) throws IOException {
        byte[] data;
        if((this.indices.size() - 1) == index) {
            data = new byte[this.filesize - this.indices.get(index)];
        } else {
            data = new byte[this.indices.get(index + 1) - this.indices.get(index)];
        }

        RandomAccessFile raf = new RandomAccessFile(this.filepath, "r");
        raf.seek(this.indices.get(index));
        raf.read(data);
        raf.close();

        String output = new String(data, StandardCharsets.UTF_8);
        int separatorIndex = output.indexOf(this.separator);
        if(separatorIndex != -1)
            // Remove the separator and every character after it from the string
            output = output.substring(0, output.indexOf(this.separator));
        return output;
    }

    /** Indexes the lines based on its own line endings. This method ignores blank lines. */
    private void indexFile() throws IOException {
        this.indices = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(this.filepath, StandardCharsets.UTF_8))) {
            // The method needs to know the length of the line ending used in the file to be able to properly calculate
            // the starting byte position of a line
            int lineEndingLength = FileUtils.getLineEnding(this.filepath).length();
            int index = 0;
            String line;
            while((line = br.readLine()) != null) {
                if(!line.isBlank()){
                    this.indices.add(index);
                    this.size++;
                }
                index += line.length() + lineEndingLength;
            }
        }
    }

    /**
     * Indexes the lines based on a custom line separator. If the content between two line separators is blank, this
     * method won't index that line.
     * @param separator the custom line separator
     * @throws IOException
     */
    private void indexFile(String separator) throws IOException {
        this.indices = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(this.filepath, StandardCharsets.UTF_8))) {
            // The method needs to know the length of the line ending used in the file to be able to properly calculate
            // the starting byte position of a line
            int lineEndingLength = FileUtils.getLineEnding(this.filepath).length();
            int currentIndex = 0;

            // The last stored index in the list
            int lastIndex = 0;

            // Used to check if every line between two separators is blank
            boolean blank = true;

            String line;
            while((line = br.readLine()) != null) {
                if(line.isBlank()) {
                    currentIndex += line.length() + lineEndingLength;
                    continue;
                }

                if(line.contains(separator)) {
                    if(!blank) {
                        this.indices.add(lastIndex);
                        this.size++;
                    }
                    currentIndex += line.length() + lineEndingLength;
                    lastIndex = currentIndex;
                    blank = true;
                    continue;
                }

                blank = false;
                currentIndex += line.length() + lineEndingLength;
            }
            if(!blank) {
                this.indices.add(lastIndex);
                this.size++;
            }
        }
    }
}
