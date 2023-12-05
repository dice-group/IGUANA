package org.aksw.iguana.cc.query.source.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;


public class FileSeparatorQuerySourceTest {
    private record SourceConfig(int size, String lineEnding, boolean overshoot, String separator) {
        @Override
        public String toString() {
            return "{ size: " + size +
                    ", overshoot: " + overshoot +
                    ", line_ending: " + StringEscapeUtils.escapeJava(lineEnding) +
                    ", separator: " + StringEscapeUtils.escapeJava(separator) + " }";
        }
    }

    private final static BiFunction<Integer, String, String> queryTemplate = (i, le) -> "Query " + i + " {" + le + "still query " + i + le + "}";

    private static Path directory;
    private static List<Arguments> cachedArguments = null;

    private static String createFileContent(int size, String lineEnding, boolean overshoot, String separator) {
        final var stringBuilder = new StringBuilder();
        int limit = overshoot ? size : size - 1;
        for (int i = 0; i < limit; i++) {
            stringBuilder.append(queryTemplate.apply(i, lineEnding)).append(separator);
        }
        if (!overshoot) {
            stringBuilder.append(queryTemplate.apply(size - 1, lineEnding));
        }
        return stringBuilder.toString();
    }

    public static List<Arguments> createTestSource() throws IOException {
        if (cachedArguments != null) {
            return cachedArguments;
        }
        List<Arguments> output = new ArrayList<>();
        int[] sizes = { 1, 1000 };
        String[] lineEndings = { "\n", "\r\n", "\r" };
        boolean[] overshoots = { false, true }; // tests if there is no empty query at the end
        String[] separators = { "\n\t\t", "\n###\n", "###", ""};
        for (int size : sizes) {
            for (String lineEnding : lineEndings) {
                for (boolean overshoot : overshoots) {
                    for (String separator : separators) {
                        String fileContent;
                        if (separator.isEmpty())
                            fileContent = createFileContent(size, lineEnding, overshoot, lineEnding + lineEnding); // make empty lines
                        else
                            fileContent = createFileContent(size, lineEnding, overshoot, separator);
                        final var filePath = Files.createTempFile(directory, "Query", ".txt");
                        Files.writeString(filePath, fileContent);
                        FileSeparatorQuerySource querySource;
                        if (separator.equals("###"))
                            querySource = new FileSeparatorQuerySource(filePath); // test default separator
                        else
                            querySource = new FileSeparatorQuerySource(filePath, separator);
                        output.add(Arguments.of(querySource, new SourceConfig(size, lineEnding, overshoot, separator)));
                    }
                }
            }
        }
        cachedArguments = output;
        return output;
    }

    @BeforeAll
    public static void createTempDirectory() throws IOException {
        directory = Files.createTempDirectory("iguana-file-line-query-source-test-dir");
    }

    @AfterAll
    public static void deleteTempDirectory() throws IOException {
        FileUtils.deleteDirectory(directory.toFile());
    }

    @Test
    public void testInitialization() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> new FileSeparatorQuerySource(null));
        assertDoesNotThrow(() -> new FileSeparatorQuerySource(Files.createTempFile(directory, "Query", ".txt"), "###"));
        final var notEmptyFile = Files.createTempFile(directory, "Query", ".txt");
        Files.writeString(notEmptyFile, "not empty");
        assertDoesNotThrow(() -> new FileSeparatorQuerySource(notEmptyFile));
        assertDoesNotThrow(() -> new FileSeparatorQuerySource(notEmptyFile, "\n\n\n"));
    }

    @ParameterizedTest(name = "[{index}] config = {1}")
    @MethodSource("createTestSource")
    public void sizeTest(FileSeparatorQuerySource querySource, SourceConfig config) throws IOException {
        assertEquals(config.size, querySource.size());
    }

    @ParameterizedTest(name = "[{index}] config = {1}")
    @MethodSource("createTestSource")
    public void getQueryTest(FileSeparatorQuerySource querySource, SourceConfig config) throws IOException {
        for (int i = 0; i < config.size; i++) {
            assertEquals(queryTemplate.apply(i, config.lineEnding), querySource.getQuery(i));
        }
    }

    @ParameterizedTest(name = "[{index}] config = {1}")
    @MethodSource("createTestSource")
    public void getAllQueriesTest(FileSeparatorQuerySource querySource, SourceConfig config) throws IOException {
        List<String> expected = IntStream.range(0, config.size).mapToObj(i -> queryTemplate.apply(i, config.lineEnding)).toList();
        assertEquals(expected, querySource.getAllQueries());
    }

    @ParameterizedTest(name = "[{index}] config = {1}")
    @MethodSource("createTestSource")
    public void getHashcodeTest(FileSeparatorQuerySource querySource, SourceConfig config) {
        assertTrue(querySource.hashCode() != 0);
    }
}