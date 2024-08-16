package org.aksw.iguana.cc.utils.files;

import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;

import java.io.*;
import java.nio.ByteBuffer;
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
	private static final XXHashFactory hasherFactory = XXHashFactory.fastestJavaInstance();
	private static final int BUFFER_SIZE = 8192;

	/**
	 * This method calculates the hashcode of the content of a file. <br/>
	 * The hashcode is calculated using the XXHash64 algorithm.
	 *
	 * @param filepath the path of the file
	 * @return 		   the hashcode of the file content
	 */
	public static int getHashcodeFromFileContent(Path filepath) {
		int hashcode;
		try (StreamingXXHash64 hasher = hasherFactory.newStreamingHash64(0);
			 InputStream is = new BufferedInputStream(Files.newInputStream(filepath), BUFFER_SIZE)) {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = (is.read(buffer))) != -1) {
				hasher.update(buffer, 0, bytesRead);
			}
			hashcode = (int) hasher.getValue();
		} catch (IOException e) {
			return 0;
		}

		return hashcode;
	}

	/**
	 * This method calculated the hashcode of a directory by hashing the content of all files in the directory. <br/>
	 * Only top-level files are considered, subdirectories are ignored. <br/>
	 * The hashcode is calculated using the XXHash64 algorithm.
	 *
	 * @param directory the path of the directory
	 * @return			the hashcode of the directory content
	 */
	public static int getHashcodeFromDirectory(Path directory) {

		int hashcode;
		try (StreamingXXHash64 hasher = hasherFactory.newStreamingHash64(0)) {
			for (Path file : Files.list(directory).sorted().toArray(Path[]::new)) {
				if (Files.isRegularFile(file)) {
					try (InputStream is = new BufferedInputStream(Files.newInputStream(file), BUFFER_SIZE)) {
						byte[] buffer = new byte[BUFFER_SIZE];
						int bytesRead;
						while ((bytesRead = (is.read(buffer))) != -1) {
							hasher.update(buffer, 0, bytesRead);
						}
					}
				}
			}
			hashcode = (int) hasher.getValue();
		} catch (IOException e) {
			return 0;
		}

		return hashcode;
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
	 * @throws IOException if an I/O error occurs opening the file
	 */
	public static String getLineEnding(Path filepath) throws IOException {
		if (filepath == null)
			throw new IllegalArgumentException("Filepath must not be null.");
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

	public static List<QueryIndex> indexStream(String separator, InputStream is) throws IOException {
		// basically Knuth-Morris-Pratt
		List<QueryIndex> indices = new ArrayList<>();


		final byte[] sepArray = separator.getBytes(StandardCharsets.UTF_8);
		final int[] prefixTable = computePrefixTable(sepArray);

		long itemStart = 0;
		long byteOffset = 0;
		int patternIndex = 0;

		// read from the stream in chunks, because the BufferedInputStream is synchronized, so single byte reads are
		// slow, which is especially bad for large files
		byte[] buffer = new byte[8192];
		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
		byte currentByte;
		int bytesRead;
		while ((bytesRead = is.read(buffer)) != -1) {
			byteBuffer.limit(bytesRead);
            while (byteBuffer.hasRemaining()) {
				currentByte = byteBuffer.get();
                // skipping fast-forward with the prefixTable
                while (patternIndex > 0 && currentByte != sepArray[patternIndex]) {
                    patternIndex = prefixTable[patternIndex - 1];
                }


                if (currentByte == sepArray[patternIndex]) {
                    patternIndex++;

                    if (patternIndex == sepArray.length) { // match found
                        patternIndex = 0;
                        final long itemEnd = byteOffset - sepArray.length + 1;
                        final long len = itemEnd - itemStart;
                        indices.add(new QueryIndex(itemStart, len));

                        itemStart = byteOffset + 1;
                    }
                }

                byteOffset++;
            }
			byteBuffer.clear();

        }

		final long itemEnd = byteOffset;
		final long len = itemEnd - itemStart;
		indices.add(new QueryIndex(itemStart, len));

		return indices;
	}
}
