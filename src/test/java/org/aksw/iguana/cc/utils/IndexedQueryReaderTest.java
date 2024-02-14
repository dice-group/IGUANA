package org.aksw.iguana.cc.utils;

import org.aksw.iguana.cc.utils.files.IndexedQueryReader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndexedQueryReaderTest {

    private static Path tempDir;

    @BeforeAll
    public static void createTestFolder() throws IOException {
        tempDir = Files.createTempDirectory("iguana-indexed-query-reader-test");
    }

    @AfterAll
    public static void removeData() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(tempDir.toFile());
    }

    private record TestData (
            Path filepath,
            String separator,
            List<String> expectedStrings
    ) {}

    private static TestData createTestFile(String content, String separator, boolean emptyBegin, boolean leadingEmptyLine, int number, int spacing) throws IOException {
        final var file = Files.createTempFile(tempDir, "line",  "queries.txt");
        final var writer = new StringWriter();
        final var lines = new ArrayList<String>();
        for (int i = (emptyBegin ? -1 : 0); i < (number * spacing) + 1; i++) {
            if (i % spacing == 0) {
                writer.append(content + i);
                lines.add(content + i);
            }
            if (leadingEmptyLine || i != number * spacing) {
                writer.append(separator);
            }
        }
        Files.writeString(file, writer.toString());
        return new TestData(file, separator, lines);
    }

    public static List<Arguments> indexWithLineEndingData() throws IOException {
        final var out = new ArrayList<Arguments>();

        final var numbers = List.of(1, 5, 10);
        final var spacings = List.of(1, 2, 5, 10, 100, 1000000);
        final var separators = List.of("\n", "\r\n", "\r");
        final var emptyBegins = List.of(true, false);
        final var leadingEmptyLines = List.of(true, false);

        // cartesian product
        for (var number : numbers) {
            for (var spacing : spacings) {
                for (var separator : separators) {
                    for (var emptyBegin : emptyBegins) {
                        for (var leadingEmptyLine : leadingEmptyLines) {
                            out.add(Arguments.of(createTestFile("line: ", separator, emptyBegin, leadingEmptyLine, number, spacing)));
                        }
                    }
                }
            }
        }

        return out;
    }

    public static List<Arguments> indexWithBlankLinesData() throws IOException {
        final var out = new ArrayList<Arguments>();

        final var numbers = List.of(1, 5, 10, 100, 10000);
        final var spacings = List.of(2);
        final var separators = List.of("\n", "\r\n", "\r");
        final var emptyBegins = List.of(false);
        final var leadingEmptyLines = List.of(false);

        // cartesian product
        for (var number : numbers) {
            for (var spacing : spacings) {
                for (var separator : separators) {
                    for (var emptyBegin : emptyBegins) {
                        for (var leadingEmptyLine : leadingEmptyLines) {
                            out.add(Arguments.of(createTestFile(String.format("this is %s line: ", separator), separator, emptyBegin, leadingEmptyLine, number, spacing)));
                            out.add(Arguments.of(createTestFile("line: ", separator, emptyBegin, leadingEmptyLine, number, spacing)));
                            out.add(Arguments.of(createTestFile(String.format("make this %s three lines %s long: ", separator, separator), separator, emptyBegin, leadingEmptyLine, number, spacing)));
                        }
                    }
                }
            }
        }

        return out;
    }

    public static List<Arguments> indexWithCustomSeparatorData() throws IOException {
        final var out = new ArrayList<Arguments>();

        final var numbers = List.of(1, 5, 10, 100, 10000);
        final var spacings = List.of(1);
        final var separators = List.of("\n", "\r\n", "\r", "\n+++\n", "\t\t\t", "test", "###$");
        final var emptyBegins = List.of(false);
        final var leadingEmptyLines = List.of(false);

        // cartesian product
        for (var number : numbers) {
            for (var spacing : spacings) {
                for (var separator : separators) {
                    for (var emptyBegin : emptyBegins) {
                        for (var leadingEmptyLine : leadingEmptyLines) {
                            out.add(Arguments.of(createTestFile("line: ", separator, emptyBegin, leadingEmptyLine, number, spacing)));
                        }
                    }
                }
            }
        }

        final var file1 = Files.createTempFile(tempDir, "iguana", "queries.txt");
        final var file2 = Files.createTempFile(tempDir, "iguana", "queries.txt");
        Files.writeString(file1, "a####$b");
        Files.writeString(file2, "a21212111b");

        out.add(Arguments.of(new TestData(file1, "###$", List.of("a#", "b"))));
        out.add(Arguments.of(new TestData(file2, "211", List.of("a2121", "1b"))));

        return out;
    }

    @ParameterizedTest
    @MethodSource("indexWithLineEndingData")
    public void testIndexingWithLineEndings(TestData data) throws IOException {
        var reader = IndexedQueryReader.make(data.filepath);
        for (int i = 0; i < data.expectedStrings.size(); i++) {
            assertEquals(data.expectedStrings.get(i), reader.readQuery(i));
        }
        assertEquals(data.expectedStrings.size(), reader.size());
    }

    @ParameterizedTest
    @MethodSource("indexWithBlankLinesData")
    public void testIndexingWithBlankLines(TestData data) throws IOException {
        IndexedQueryReader reader = IndexedQueryReader.makeWithEmptyLines(data.filepath);
        for (int i = 0; i < data.expectedStrings.size(); i++) {
            assertEquals(data.expectedStrings.get(i), reader.readQuery(i));
        }
        assertEquals(data.expectedStrings.size(), reader.size());
    }

    @ParameterizedTest
    @MethodSource("indexWithCustomSeparatorData")
    public void testIndexingWithCustomSeparator(TestData data) throws IOException {
        IndexedQueryReader reader = IndexedQueryReader.makeWithStringSeparator(data.filepath, data.separator);
        for (int i = 0; i < data.expectedStrings.size(); i++) {
            assertEquals(data.expectedStrings.get(i), reader.readQuery(i));
        }
        assertEquals(data.expectedStrings.size(), reader.size());
    }
}
