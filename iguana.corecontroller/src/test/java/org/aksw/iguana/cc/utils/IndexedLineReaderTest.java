package org.aksw.iguana.cc.utils;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class IndexedLineReaderTest {

    @RunWith(Parameterized.class)
    public static class ParameterizedTest {

        IndexedLineReader reader;

        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"src/test/resources/readLineTestFile1.txt"},
                    {"src/test/resources/readLineTestFile2.txt"},
                    {"src/test/resources/readLineTestFile3.txt"}
            });
        }

        public ParameterizedTest(String path) throws IOException {
            reader = IndexedLineReader.make(path);
        }

        @Test
        public void testIndexingWithLineEndings() throws IOException {
            assertEquals("line 1", reader.readLine(0));
            assertEquals("line 2", reader.readLine(1));
            assertEquals("line 3", reader.readLine(2));
            assertEquals("line 4", reader.readLine(3));
        }
    }

    public static class NonParameterizedTest {
        @Test
        public void testIndexingWithCustomSeparator() throws IOException {
            IndexedLineReader reader1 = IndexedLineReader.makeWithStringSeparator("src/test/resources/utils/indexingtestfile1.txt", "#####");

            assertEquals("line 1", reader1.readLine(0));
            assertEquals("\r\nline 2", reader1.readLine(1));

            IndexedLineReader reader2 = IndexedLineReader.makeWithStringSeparator("src/test/resources/utils/indexingtestfile2.txt", "#####");
            assertEquals("\r\nline 0", reader2.readLine(0));
            assertEquals("line 1", reader2.readLine(1));
            assertEquals("\r\nline 2", reader2.readLine(2));
        }

        @Test
        public void testIndexingWithBlankLines() throws IOException {
            IndexedLineReader reader = IndexedLineReader.makeWithBlankLines("src/test/resources/utils/indexingtestfile3.txt");

            assertEquals(" line 1\r\nline 2", reader.readLine(0));
            assertEquals("line 3", reader.readLine(1));
            assertEquals("line 4\r\nline 5", reader.readLine(2));
        }
    }
}
