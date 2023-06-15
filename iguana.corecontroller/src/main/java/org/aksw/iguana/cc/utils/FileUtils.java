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
		try(FileReader fr = new FileReader(filepath);
			BufferedReader br = new BufferedReader(fr)) {
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
}
