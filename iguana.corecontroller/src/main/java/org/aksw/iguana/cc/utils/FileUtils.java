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
			try (FileInputStream fis = new FileInputStream(filename)) {
				InputStream is = new BufferedInputStream(fis, BUFFER_SIZE);
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
		} else {
			String line = "";
			int count = 0;
			try(FileReader fr = new FileReader(filename)) {
				BufferedReader br = new BufferedReader(fr);
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

		try(FileReader fr = new FileReader(file)) {
			BufferedReader br = new BufferedReader(fr);
			while ((line = br.readLine()) != null) {
				if (!line.isBlank()) {
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
		try(FileReader fr = new FileReader(filepath)) {
			BufferedReader br = new BufferedReader(fr);
			char c;
			while ((c = (char) br.read()) != (char) -1) {
				if (c == '\n')
					return "\n";
				else if (c == '\r') {
					if ((char) br.read() == '\n')
						return "\r\n";
					return "\r";
				}
			}
		}

		// fall back if there is no line end in the file
		return System.lineSeparator();
	}

	public static BufferedReader getBufferedReader(File queryFile) throws FileNotFoundException {
		return new BufferedReader(new FileReader(queryFile));
	}
}
