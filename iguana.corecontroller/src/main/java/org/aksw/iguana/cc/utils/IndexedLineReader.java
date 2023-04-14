package org.aksw.iguana.cc.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class creates an object, that indexes the starting positions of the lines inside a file for faster access. <br/>
 * The indexing happens on either:
 * <ul>
 *     <li>the file's line endings, or</li>
 *     <li>a custom separator</li>
 * </ul>
 * This class does not index blank lines.
 */
public class IndexedLineReader {

    /** This list stores the indices of the bytes, at which a new line starts. (Stores Integers, so it only supports
     *  files that are roughly 2GB big.)*/
    private ArrayList<Integer[]> indices;

    private String filepath;

    /**
     * An optional object that contains the separator, that is used to index lines of the file. If the separator is
     * blank, blank lines will be used for separation. Otherwise, if no separator is given, the constructor will index
     * every non-blank line.
     */
    private Optional<String> separator;

    /** Number of indexed lines. */
    private int size;

    /** The line terminator, that has been used inside the file. */
    private String lineEnding;

    /**
     * This constructor indexes the lines in the given file based on the file's line ending.
     * @param filepath path to the file
     * @throws IOException
     */
    public IndexedLineReader(String filepath) throws IOException {
        this(filepath, null);
    }

    /**
     * Creates an object that indexes the lines in the given file based on the specified line separator. If the given
     * separator is empty, the object will instead index the lines based on empty lines.
     * @param filepath path to the file
     * @throws IOException
     */
    public IndexedLineReader(String filepath, String separator) throws IOException {
        this.filepath = filepath;
        if(separator != null)
            this.separator = Optional.of(separator);
        else
            this.separator = Optional.empty();

        this.lineEnding = FileUtils.getLineEnding(this.filepath);

        if(this.separator.isEmpty()) {
            this.indexFile();
        }
        else if(this.separator.get().isEmpty()) {
            this.indexFileWithBlankLines();
        }
        else {
            this.indexFile(separator);
        }
    }

    /**
     * This method reads a line from the file at a given index. It will skip every byte of data, that is written in the
     * file before the line. <br/>
     * If the lines were indexed based on a custom separator, this method returns every line inside the file
     * between two of the given separators (the beginning and ending of the file count as separators too).
     * @param index the index of the line
     * @return the searched line
     * @throws IOException
     */
    public String readLine(int index) throws IOException {
        if(this.separator.isPresent()) {
            return readLinesBetweenSeparator(index);
        }

        RandomAccessFile raf = new RandomAccessFile(this.filepath, "r");
        raf.seek(this.indices.get(index)[0]);
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
     * This method reads the lines between the given index and the next index (or file end). It begins reading every
     * line, starting at the given index, until it reaches the position of the next index. It cuts out the line,
     * that contains the actual separator (and everything after it).
     * @param index
     * @return the string consisting of the lines between two separators
     * @throws IOException
     */
    private String readLinesBetweenSeparator(int index) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(this.filepath, "r");
        raf.seek(this.indices.get(index)[0]);
        StringBuilder output = new StringBuilder();

        // This is the byte position of the next index or end of file. It's used to know, for how long the lines can be
        // read the file starting at the position of this.indices.get(index).
        int nextLinePosition = this.indices.get(index)[1];
        String lastLine = raf.readLine();
        if(raf.getFilePointer() >= nextLinePosition)
            return lastLine;

        String currentLine;
        while((currentLine = raf.readLine()) != null) {
            output.append(lastLine).append(this.lineEnding);
            lastLine = currentLine;

            if(raf.getFilePointer() >= nextLinePosition ||
                    (this.separator.get().isBlank() && currentLine.isBlank())) {
                // Break the while-loop, when the line, that contains the separator has been read, or if the current
                // line is blank, when the separator is blank.
                break;
            }
        }
        output.append(lastLine);
        return output.toString();
    }

    /** Indexes the lines based on its own line endings. This method ignores blank lines.
     * @throws IOException
     */
    private void indexFile() throws IOException {
        this.indices = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(this.filepath, StandardCharsets.UTF_8))) {
            // The method needs to know the length of the line ending used in the file to be able to properly calculate
            // the starting byte position of a line
            int lineEndingLength = lineEnding.length();
            int index = 0;
            String line;
            while((line = br.readLine()) != null) {
                if(!line.isBlank()){
                    this.indices.add(new Integer[] {index});
                    this.size++;
                }
                index += line.length() + lineEndingLength;
            }
        }
    }

    /**
     * Indexes the lines based on a blank line. Multiple following blank lines will be ignored.
     * @throws IOException
     */
    private void indexFileWithBlankLines() throws IOException {
        this.indices = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(this.filepath, StandardCharsets.UTF_8))) {
            // The method needs to know the length of the line ending used in the file to be able to properly calculate
            // the starting byte position of a line
            int lineEndingLength = lineEnding.length();
            int currentIndex = 0;

            // The last stored index in the list
            int lastIndex = 0;

            // Used to check if every line between two separators is blank
            boolean blank = true;

            String line;
            while((line = br.readLine()) != null) {
                if(line.isBlank()) {
                    if(!blank) {
                        this.indices.add(new Integer[]{lastIndex, currentIndex});
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
                this.indices.add(new Integer[]{lastIndex, currentIndex});
                this.size++;
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
            int lineEndingLength = lineEnding.length();
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
                        this.indices.add(new Integer[]{lastIndex, currentIndex});
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
                this.indices.add(new Integer[]{lastIndex, currentIndex});
                this.size++;
            }
        }
    }
}
