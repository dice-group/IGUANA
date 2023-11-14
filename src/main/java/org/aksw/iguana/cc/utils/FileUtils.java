package org.aksw.iguana.cc.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Methods to work easier with Files.
 *
 * @author f.conrads
 *
 */
public class FileUtils {

	public static int getHashcodeFromFileContent(Path filepath) {
		int hashcode;
		try {
			String fileContents = readFile(filepath);
			hashcode = Math.abs(fileContents.hashCode());
		} catch (IOException e) {
			hashcode = 0;
		}
		return hashcode;
	}

	public static String readFile(Path path) throws IOException {
		return Files.readString(path, StandardCharsets.UTF_8);
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
	public static String getLineEnding(Path filepath) throws IOException {
		try(BufferedReader br = Files.newBufferedReader(filepath)) {
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

	private static int[] computePrefixTable(byte[] pattern) {
		int[] prefixTable = new int[pattern.length];

		int prefixIndex = 0;
		for (int i = 1; i < pattern.length; i++) {
			while (prefixIndex > 0 && pattern[prefixIndex] != pattern[i]) {
				prefixIndex = prefixTable[prefixIndex - 1];
			}

			if (pattern[prefixIndex] == pattern[i]) {
				prefixIndex++;
			}

			prefixTable[i] = prefixIndex;
		}

		return prefixTable;
	}

	public static List<long[]> indexStream(String separator, InputStream is) throws IOException {
		// basically Knuth-Morris-Pratt
		List<long[]> indices = new ArrayList<>();


		final byte[] sepArray = separator.getBytes(StandardCharsets.UTF_8);
		final int[] prefixTable = computePrefixTable(sepArray);

		long itemStart = 0;

		long byteOffset = 0;
		int patternIndex = 0;
		byte[] currentByte = new byte[1];
		while (is.read(currentByte) == 1) {
			// skipping fast-forward with the prefixTable
			while (patternIndex > 0 && currentByte[0] != sepArray[patternIndex]) {
				patternIndex = prefixTable[patternIndex - 1];
			}


			if (currentByte[0] == sepArray[patternIndex]) {
				patternIndex++;

				if (patternIndex == sepArray.length) { // match found
					patternIndex = 0;
					final long itemEnd = byteOffset - sepArray.length + 1;
					final long len = itemEnd - itemStart;
					indices.add(new long[]{itemStart, len});

					itemStart = byteOffset + 1;
				}
			}

			byteOffset++;
		}

		final long itemEnd = byteOffset;
		final long len = itemEnd - itemStart;
		indices.add(new long[]{itemStart, len});

		return indices;
	}
}
