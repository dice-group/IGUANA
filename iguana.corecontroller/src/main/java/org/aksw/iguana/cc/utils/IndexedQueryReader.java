package org.aksw.iguana.cc.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This class creates objects, that index the start positions characters in between two given separators.
 * A separator can be, for example "\n", which is the equivalent of indexing every line. <br/>
 * The beginning and the end of the file count as separators too.
 * <br/>
 * Blank content in between two separators won't be indexed. <br/>
 * The start positions and the length of each indexed content will be stored in an internal array for later accessing.
 */
public class IndexedQueryReader {

    /** This list stores the start position and the length of each indexed content. */
    private ArrayList<Long[]> indices;

    /** The file whose content should be indexed. */
    private final File file;

    /**
     * Indexes each content in between two of the given separators (including the beginning and end of the file). The
     * given separator isn't allowed to be blank.
     * @param filepath path to the file
     * @param separator the separator line that is used in the file (isn't allowed to be blank)
     * @return reader to access the indexed content
     * @throws IllegalArgumentException the given separator was blank
     * @throws IOException
     */
    public static IndexedQueryReader makeWithStringSeparator(String filepath, String separator) throws IOException {
        if(separator.isBlank())
            throw new IllegalArgumentException("Separator for makeWithStringSeparator can not be blank.");
        return new IndexedQueryReader(filepath, separator);
    }

    /**
     * Indexes every bundle of lines inside the file, that are in between two empty lines (including the beginning and
     * end of the file). <br/>
     * It uses the doubled line ending of the file as a separator, for example "\n\n".
     * @param filepath path to the file
     * @return reader to access the indexed content
     * @throws IOException
     */
    public static IndexedQueryReader makeWithEmptyLines(String filepath) throws IOException {
        String lineEnding = FileUtils.getLineEnding(filepath);
        return new IndexedQueryReader(filepath, lineEnding + lineEnding);
    }

    /**
     * Indexes every non-blank line inside the given file. It uses the line ending of the file as a separator.
     * @param filepath path to the file
     * @return reader to access the indexed lines
     * @throws IOException
     */
    public static IndexedQueryReader make(String filepath) throws IOException {
        return new IndexedQueryReader(filepath, FileUtils.getLineEnding(filepath));
    }

    /**
     * Creates an object that indexes each content in between two of the given separators (including the beginning and
     * end of the given file). <br/>
     * @param filepath path to the file
     * @param separator the separator for each bundle
     * @throws IOException
     */
    private IndexedQueryReader(String filepath, String separator) throws IOException {
        this.file = new File(filepath);
        this.indexFile(separator);
    }

    /**
     * Returns the indexed content with the given index.
     * @param index the index of the searched content
     * @return the searched content
     * @throws IOException
     */
    public String readQuery(int index) throws IOException {
        // Indexed queries can't be larger than ~2GB
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
     * This method returns a list of strings that contains every indexed content.
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
     * Returns the number of indexed content.
     * @return number of indexed objects
     */
    public int size() {
        return this.indices.size();
    }

    /**
     * Indexes every content in between two of the given separator. The beginning and the end of the file count as
     * separators too.
     * @param separator the custom separator
     * @throws IOException
     */
    private void indexFile(String separator) throws IOException {
        this.indices = new ArrayList<>();
        final char[] sepArray = separator.toCharArray();
        try(FileReader fr = new FileReader(this.file, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(fr)) {
            // starting position of the last indexed query
            long lastIndex = 0;
            long currentIndex = 0;

            int counter = 0;
            int currentChar;
            boolean isWhiteSpace = true;

            // if the current character matches with sepArray[counter], this will store the read string that matched
            // with a beginning substring of the separator
            StringBuilder readString = new StringBuilder();

            // If matching occurred but failed, this will store the characters from readString except the first
            // character. They will need to be checked again to see, if a separator is beginning from these characters.
            // This buffer is used to prevent having to read an already read character again from the file.
            Queue<Character> readChars = new LinkedList<>();

            while((currentChar = br.read()) != -1) {
                if(currentChar == (int) sepArray[counter]) {
                    readString.append((char) currentChar);
                    if(++counter >= separator.length()) {
                        if(!isWhiteSpace) {
                            this.indices.add(new Long[]{lastIndex, currentIndex - lastIndex});
                        }
                        currentIndex += counter;
                        counter = 0;
                        lastIndex = currentIndex;
                        isWhiteSpace = true;
                        readString = new StringBuilder();
                        readChars = new LinkedList<>();
                    }
                } else {
                    if(counter != 0) {
                        // first character from readString was already checked for a separator
                        for(int i = 1; i < readString.length(); i++) {
                            readChars.offer(readString.charAt(i));
                        }
                        // last read char that didn't match is not inside readString anymore, thus needs to be added
                        // manually here
                        readChars.offer((char) currentChar);
                        readString = new StringBuilder();
                        counter = 0;
                    }
                    if(isWhiteSpace && !Character.isWhitespace(currentChar)) {
                        isWhiteSpace = false;
                    }
                    currentIndex++;
                }

                // If there are characters in the buffer
                while(!readChars.isEmpty()) {
                    currentChar = (int) readChars.poll();
                    readString.append((char) currentChar);
                    if(currentChar == (int) sepArray[counter]) {
                        counter++;
                    } else {
                        // First character from readString was already checked for separator
                        for(int i = 1; i < readString.length(); i++) {
                            readChars.offer(readString.charAt(i));
                        }
                        readString = new StringBuilder();
                        currentIndex++;
                        counter = 0;
                    }
                }
            }
            if(!isWhiteSpace) {
                this.indices.add(new Long[]{lastIndex, currentIndex - lastIndex});
            }
        }
    }
}
