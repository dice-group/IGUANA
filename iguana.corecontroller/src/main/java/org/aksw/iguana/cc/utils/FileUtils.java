package org.aksw.iguana.cc.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Methods to work easier with Files.
 * 
 * @author f.conrads
 *
 */
public class FileUtils {

	/**
	 * Counts the lines in a file efficiently. (only if the line ending is "\n") <br/>
	 * Source: <a href="http://stackoverflow.com/a/453067/2917596">http://stackoverflow.com/a/453067/2917596</a>
	 *
	 * @param filename file to count lines of
	 * @return number of lines in the given file
	 * @throws IOException
	 */
	public static int countLines(File filename) throws IOException {
		if(getLineEnding((filename.getAbsolutePath())).equals("\n")) {
			final int BUFFER_SIZE = 8192;
			try (InputStream is = new BufferedInputStream(new FileInputStream(filename), BUFFER_SIZE)) {
				byte[] c = new byte[BUFFER_SIZE];
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
		else {
			String line = "";
			int count = 0;
			try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
				while ((line = br.readLine()) != null) {
					if (!line.isEmpty()) {
						count++;
					}
				}
			}
			return count;
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

	/**
	 * This method detects and returns the line-ending used in a file. <br/>
	 * It reads the whole first line until it detects one of the following line-endings:
	 * <ul>
	 *     <li>\r\n - Windows</li>
	 *     <li>\n - Linux</li>
	 *     <li>\r - old macOS</li>
	 * </ul>
	 *
	 * If the file doesn't contain a line ending, it defaults to <code>System.lineSeparator()</code>.
	 *
	 * @param filepath this string that contains the path of the file
	 * @return the line ending used in the given file
	 * @throws IOException
	 */
	public static String getLineEnding(String filepath) throws IOException {
		int lineEndingIndex = 0;
		try(BufferedReader br = new BufferedReader(new FileReader(filepath))) {
			// readline consumes the line endings mentioned in the javadoc, thus the length of a line equals the index
			// of the line's ending
			lineEndingIndex = br.readLine().length();
		}

		// assumes that line endings can have a maximum of 2 characters
		byte[] buffer = new byte[2];
		int numberOfreadChars = 0;
		try(InputStream is = new BufferedInputStream(new FileInputStream(filepath))) {
			is.skip(lineEndingIndex);
			numberOfreadChars = is.read(buffer);
		}

		// in the case, that the file contains no line ending
		if(numberOfreadChars == 0) {
			return System.lineSeparator();
		}

		// converts the buffer to a string
		String bufferString = "";
		for(int i = 0; i < numberOfreadChars; i++){
			bufferString += (char) buffer[i];
		}

		// The regex pattern "\R" searches for every type of line ending, the result of the pattern matching is the
		// result of this method.
		// The pattern matching is done here, in case that the line ending has only one character. In that case
		// bufferString can still contain 2 characters (i.e. the line ending is "\n" and after the first line there is
		// a second, non-empty line, this results in bufferString equaling "\n" + the first character of the second
		// line)
		Pattern pattern = Pattern.compile("\\R");
		Matcher matcher = pattern.matcher(bufferString);
		if(matcher.find()) {
			return matcher.group();
		}
		else {
			// if for some reason, the matcher still doesn't find a line ending
			return System.lineSeparator();
		}
	}
}
