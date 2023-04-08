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
	 * http://stackoverflow.com/a/453067/2917596
	 * 
	 * 
	 * @param filename File to count lines of
	 * @return No. of lines in File
	 * @throws IOException
	 */
	public static int countLines(File filename) throws IOException {
		try (InputStream is = new BufferedInputStream(new FileInputStream(filename))) {

			byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
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
	 * Returns a line at a given position of a File. <br/>
	 * This method ignores every empty line, therefore the parameter <code>pos</code> references the n-th non-empty line.
	 *
	 * @param index the position of a non-empty line which should be returned
	 * @param file 	the file to read from
	 * @return the line at the given position
	 * @throws IOException
	 */
	public static String readLineAt(int index, File file) throws IOException {
		String line = "";
		int count = 0;

		try(BufferedReader br = new BufferedReader(new FileReader(file))) {
			while ((line = br.readLine()) != null) {
				if (!line.isEmpty()) {
					if (count == index) {
						return line;
					}
					count++;
				}
			}
		}
		return "";
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
}
