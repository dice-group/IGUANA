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
        public void testIndexingWithCustomSeparator() throws IOException {
            IndexedQueryReader reader1 = IndexedQueryReader.makeWithStringSeparator("src/test/resources/utils/indexingtestfile1.txt", "#####\r\n");

            assertEquals("line 1\r\n", reader1.readQuery(0));
            assertEquals("\r\nline 2\r\n", reader1.readQuery(1));

            IndexedQueryReader reader2 = IndexedQueryReader.makeWithStringSeparator("src/test/resources/utils/indexingtestfile2.txt", "#####\r\n");
            assertEquals("\r\nline 0\r\n", reader2.readQuery(0));
            assertEquals("line 1\r\n", reader2.readQuery(1));
            assertEquals("\r\nline 2", reader2.readQuery(2));
        }

        @Test
        public void testIndexingWithBlankLines() throws IOException {
            IndexedQueryReader reader = IndexedQueryReader.makeWithEmptyLines("src/test/resources/utils/indexingtestfile3.txt");

            assertEquals(" line 1\r\nline 2", reader.readQuery(0));
            assertEquals("line 3", reader.readQuery(1));
            assertEquals("line 4\r\nline 5", reader.readQuery(2));
        }
    }
}
