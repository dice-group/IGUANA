package org.aksw.iguana.cc.utils;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class IndexedQueryReaderTest {

    @RunWith(Parameterized.class)
    public static class ParameterizedTest {

        IndexedQueryReader reader;

        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"src/test/resources/readLineTestFile1.txt"},
                    {"src/test/resources/readLineTestFile2.txt"},
                    {"src/test/resources/readLineTestFile3.txt"}
            });
        }

        public ParameterizedTest(String path) throws IOException {
            reader = IndexedQueryReader.make(path);
        }

        @Test
        public void testIndexingWithLineEndings() throws IOException {
            assertEquals("line 1", reader.readQuery(0));
            assertEquals("line 2", reader.readQuery(1));
            assertEquals("line 3", reader.readQuery(2));
            assertEquals("line 4", reader.readQuery(3));
        }
    }

    public static class NonParameterizedTest {
        @Test
        public void testIndexingWithBlankLines() throws IOException {
            IndexedQueryReader reader = IndexedQueryReader.makeWithEmptyLines("src/test/resources/utils/indexingtestfile3.txt");
            String le = FileUtils.getLineEnding("src/test/resources/utils/indexingtestfile3.txt");

            assertEquals(" line 1" + le + "line 2", reader.readQuery(0));
            assertEquals("line 3", reader.readQuery(1));
            assertEquals("line 4" + le + "line 5", reader.readQuery(2));
        }
    }

    @RunWith(Parameterized.class)
    public static class TestCustomSeparator {
        private static class TestData {
            public String filepath;
            public String separator;
            public String[] expectedStrings;

            public TestData(String filepath, String separator, String[] expectedStrings) {
                this.filepath = filepath;
                this.separator = separator;
                this.expectedStrings = expectedStrings;
            }
        }

        private TestData data;

        public TestCustomSeparator(TestData data) {
            this.data = data;
        }

        @Parameterized.Parameters
        public static Collection<TestData> data() throws IOException {
            // all the files should have the same line ending
            String le = FileUtils.getLineEnding("src/test/resources/utils/indexingtestfile1.txt");
            return List.of(
                    new TestData("src/test/resources/utils/indexingtestfile1.txt", "#####" + le, new String[]{"line 1" + le, le + "line 2" + le}),
                    new TestData("src/test/resources/utils/indexingtestfile2.txt", "#####" + le, new String[]{"line 0" + le, "line 1" + le + "#####"}),
                    new TestData("src/test/resources/utils/indexingtestfile4.txt", "###$", new String[]{"a#", "b"}),
                    new TestData("src/test/resources/utils/indexingtestfile5.txt", "211", new String[]{"a21", "b"})
            );
        }

        @Test
        public void testIndexingWithCustomSeparator() throws IOException {
            IndexedQueryReader reader = IndexedQueryReader.makeWithStringSeparator(this.data.filepath, this.data.separator);
            for (int i = 0; i < this.data.expectedStrings.length; i++) {
                String read = reader.readQuery(i);
                assertEquals(this.data.expectedStrings[i], read);
            }
            assertEquals(this.data.expectedStrings.length, reader.readQueries().size());
        }
    }
}
