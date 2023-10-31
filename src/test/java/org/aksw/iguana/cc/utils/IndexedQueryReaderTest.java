package org.aksw.iguana.cc.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndexedQueryReaderTest {

    @org.junit.jupiter.params.ParameterizedTest
    @ValueSource(strings = {"src/test/resources/readLineTestFile1.txt", "src/test/resources/readLineTestFile2.txt", "src/test/resources/readLineTestFile3.txt"})
    public void testIndexingWithLineEndings(String path) throws IOException {
        var reader = IndexedQueryReader.make(Paths.get(path));
        assertEquals("line 1", reader.readQuery(0));
        assertEquals("line 2", reader.readQuery(1));
        assertEquals("line 3", reader.readQuery(2));
        assertEquals("line 4", reader.readQuery(3));
    }

    @Test
    public void testIndexingWithBlankLines() throws IOException {
        IndexedQueryReader reader = IndexedQueryReader.makeWithEmptyLines(Paths.get("src/test/resources/utils/indexingtestfile3.txt"));
        String le = FileUtils.getLineEnding(Paths.get("src/test/resources/utils/indexingtestfile3.txt"));

        assertEquals(" line 1" + le + "line 2", reader.readQuery(0));
        assertEquals("line 3", reader.readQuery(1));
        assertEquals("line 4" + le + "line 5", reader.readQuery(2));
    }

    private record TestData (
        Path filepath,
        String separator,
        String[] expectedStrings
    ) {}

    public static Collection<TestData> data() throws IOException {
        // all the files should have the same line ending
        String le = FileUtils.getLineEnding(Path.of("src/test/resources/utils/indexingtestfile1.txt"));
        return List.of(
                new TestData(Path.of("src/test/resources/utils/indexingtestfile1.txt"), "#####" + le, new String[]{"line 1" + le, le + "line 2" + le}),
                new TestData(Path.of("src/test/resources/utils/indexingtestfile2.txt"), "#####" + le, new String[]{"line 0" + le, "line 1" + le + "#####"}),
                new TestData(Path.of("src/test/resources/utils/indexingtestfile4.txt"), "###$", new String[]{"a#", "b"}),
                new TestData(Path.of("src/test/resources/utils/indexingtestfile5.txt"), "211", new String[]{"a21", "b"})
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testIndexingWithCustomSeparator(TestData data) throws IOException {
        IndexedQueryReader reader = IndexedQueryReader.makeWithStringSeparator(data.filepath, data.separator);
        for (int i = 0; i < data.expectedStrings.length; i++) {
            String read = reader.readQuery(i);
            assertEquals(data.expectedStrings[i], read);
        }
        assertEquals(data.expectedStrings.length, reader.readQueries().size());
    }
}
