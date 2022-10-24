package org.aksw.iguana.cc.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Methods to work easier with Files.
 * 
 * @author f.conrads
 *
 */
public class FileUtils {

	/**
	 * Counts the lines in a file efficently Props goes to:
	 * <a href="http://stackoverflow.com/a/453067/2917596">...</a>
	 *
	 * @param filename File to count lines of
	 * @return No. of lines in File
	 * @throws IOException
	 */
	public static int countLines(File filename) throws IOException {
		try (InputStream is = new BufferedInputStream(new FileInputStream(filename))) {

			byte[] c = new byte[1024];
			int count = 0;
			int readChars;
			boolean empty = true;
			byte lastChar = '\n';
			while ((readChars = is.read(c)) != -1) {
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						// Check if line was empty
						if (lastChar != '\n') {
							++count;
						}
					} else {
						empty = false;
					}
					lastChar = c[i];
				}
			}
			if (lastChar != '\n') {
				count++;
			}
			return (count == 0 && !empty) ? 1 : count;
		}
	}

	/**
	 * Returns a line at a given position of a File
	 * 
	 * @param pos      line which should be returned
	 * @param filename File in which the queries are stated
	 * @return line at pos
	 * @throws IOException
	 */
	public static String readLineAt(int pos, File filename) throws IOException {
		try (InputStream is = new BufferedInputStream(new FileInputStream(filename))) {
			StringBuilder line = new StringBuilder();

			byte[] c = new byte[1024];
			int count = 0;
			int readChars;
			byte lastChar = '\n';
			while ((readChars = is.read(c)) != -1) {
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						// Check if line was empty
						if (lastChar != '\n') {
							++count;
						}
					} else if (count == pos) {
						// Now the line
						line.append((char) c[i]);
					}
					lastChar = c[i];
				}
			}

			return line.toString();
		} 
	}

	public static int getHashcodeFromFileContent(String filepath) {
		int hashcode;
		try {
			String fileContents = readFile(filepath);
			hashcode = Math.abs(fileContents.hashCode());
		} catch (IOException e) {
			hashcode = 0;
		}
		return hashcode;
	}

	public static String readFile(String path) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, StandardCharsets.UTF_8);
	}

	public static BufferedReader getBufferedReader(File queryFile) throws FileNotFoundException {
		return new BufferedReader(new FileReader(queryFile));
	}
}
