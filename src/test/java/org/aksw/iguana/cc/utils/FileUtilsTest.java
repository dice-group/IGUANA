package org.aksw.iguana.cc.utils;

import net.jpountz.xxhash.XXHashFactory;
import org.aksw.iguana.cc.utils.files.FileUtils;
import org.aksw.iguana.cc.utils.files.QueryIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static java.nio.file.Files.createTempFile;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.jupiter.api.Assertions.*;

public class FileUtilsTest {
    public static Path createTestFileWithLines(List<String> content, String lineEnding) throws IOException {
        final var file = createTempFile("getHashTest", ".txt");
        for (String s : content) {
            writeStringToFile(file.toFile(), s + lineEnding, StandardCharsets.UTF_8, true);
        }
        file.toFile().deleteOnExit();
        return file;
    }

    public static Path createTestFileWithContent(String content) throws IOException {
        final var file = createTempFile("getHashTest", ".txt");
        writeStringToFile(file.toFile(), content, StandardCharsets.UTF_8, false);
        file.toFile().deleteOnExit();
        return file;
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r", "\r\n"})
    public void testGetLineEndings(String ending) throws IOException {
        final var file = createTestFileWithLines(List.of("a", "b"), ending);
        assertEquals(FileUtils.getLineEnding(file), ending);
    }

    public record IndexTestData(
            String content, // String to be separated
            String separator,
            List<long[]> indices // List of [offset, length] arrays
    ) {}

    public static Collection<IndexTestData> data() {
        return List.of(
                new IndexTestData("", "a", Arrays.asList(new long[]{0, 0})),
                new IndexTestData("a", "a", Arrays.asList(new long[]{0, 0}, new long[]{1, 0})),
                new IndexTestData("abc", "b", Arrays.asList(new long[]{0, 1}, new long[]{2, 1})),
                new IndexTestData("1\n2", "\n", Arrays.asList(new long[]{0, 1}, new long[]{2, 1})),
                new IndexTestData("1\t2", "\t", Arrays.asList(new long[]{0, 1}, new long[]{2, 1})),
                new IndexTestData("abcbd", "b", Arrays.asList(new long[]{0, 1}, new long[]{2, 1}, new long[]{4, 1})),
                new IndexTestData("aab", "ab", Arrays.asList(new long[]{0, 1}, new long[]{3, 0})),
                new IndexTestData("aaaabaabaa", "ab", Arrays.asList(new long[]{0, 3}, new long[]{5, 1}, new long[]{8, 2})),
                new IndexTestData("1\n\t\n2", "\n\t\n", Arrays.asList(new long[]{0, 1}, new long[]{4, 1}))
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testIndexingStrings(IndexTestData data) throws IOException {
        List<QueryIndex> index = FileUtils.indexStream(data.separator, new ByteArrayInputStream(data.content.getBytes()));
        assertEquals(data.indices.size(), index.size());
        for (int i = 0; i < index.size(); i++) {
            assertEquals(data.indices.get(i)[0], index.get(i).filePosition());
            assertEquals(data.indices.get(i)[1], index.get(i).queryLength());
        }
    }

    @Test
    public void getHashTest() throws IOException {
        final var hasherFactory = XXHashFactory.fastestJavaInstance();
        for (int i = 0; i < 10; i++) {
            String content = UUID.randomUUID().toString();
            final var file = createTestFileWithContent(content);
            final var data = content.getBytes(StandardCharsets.UTF_8);
            final var hasher = hasherFactory.hash64();
            final int expected = (int) hasher.hash(data, 0, data.length, 0);

            final int actual = FileUtils.getHashcodeFromFileContent(file);
            assertEquals(expected, actual);

            final int actual2 = FileUtils.getHashcodeFromFileContent(file);
            assertEquals(expected, actual2);
        }

        final var directory = Files.createTempDirectory("getHashTest");
        for (int i = 0; i < 10; i++) {
            final var file = createTestFileWithContent(UUID.randomUUID().toString());
            Files.move(file, directory.resolve(file.getFileName()));
        }
        final int actual = FileUtils.getHashcodeFromDirectory(directory);
        final int actual2 = FileUtils.getHashcodeFromDirectory(directory);
        assertEquals(actual, actual2);
        org.apache.commons.io.FileUtils.deleteDirectory(directory.toFile());
    }
}
