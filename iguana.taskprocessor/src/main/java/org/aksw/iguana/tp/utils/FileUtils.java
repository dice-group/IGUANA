package org.aksw.iguana.tp.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Methods to work easier with Files.
 * 
 * @author f.conrads
 *
 */
public class FileUtils {

    /**
     * Counts the lines in a file efficently 
     * Props goes to: http://stackoverflow.com/a/453067/2917596
     * 
     * 
     * @param filename File to count lines of
     * @return No. of lines in File
     * @throws IOException
     */
    public static int countLines(File filename) throws IOException {
	InputStream is = new BufferedInputStream(new FileInputStream(filename));
	try {
	    byte[] c = new byte[1024];
	    int count = 0;
	    int readChars = 0;
	    boolean empty = true;
	    byte lastChar='\n';
	    while ((readChars = is.read(c)) != -1) {
		for (int i = 0; i < readChars; ++i) {
		    if (c[i] == '\n') {
			//Check if line was empty
			if(lastChar != '\n'){
			    ++count;
			}
		    }
		    else{
			empty = false;
		    }
		    lastChar = c[i];
		}
	    }
	    if(lastChar != '\n'){
		count++;
	    }
	    return (count == 0 && !empty) ? 1 : count;
	} finally {
	    is.close();
	}
    }

    /**
     * Returns a line at a given position of a File
     * 
     * 
     * @param pos line which should be returned
     * @param filename File in which the queries are stated
     * @return line at pos
     * @throws IOException
     */
    public static String readLineAt(int pos, File filename) throws IOException {
	InputStream is = new BufferedInputStream(new FileInputStream(filename));
	StringBuilder line = new StringBuilder();
	try {
	    byte[] c = new byte[1024];
	    int count = 0;
	    int readChars = 0;
	    byte lastChar='\n';
	    while ((readChars = is.read(c)) != -1) {
		for (int i = 0; i < readChars; ++i) {
		    if (c[i] == '\n') {
			//Check if line was empty
			if(lastChar != '\n'){
			    ++count;
			}
		    }
		    else if(count == pos){
			//Now the line 
			line.append((char)c[i]);
		    }
		    lastChar = c[i];
		}
	    }
	    
	    return line.toString();
	} finally {
	    is.close();
	}
    }
}
