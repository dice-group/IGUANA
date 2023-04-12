package org.aksw.iguana.cc.utils;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class IndexedLineReaderTest {

    @Test
    public void testIndexingWithLineEndings() throws IOException {
        IndexedLineReader reader1 = new IndexedLineReader("src/test/resources/readLineTestFile1.txt");
        IndexedLineReader reader2 = new IndexedLineReader("src/test/resources/readLineTestFile2.txt");
        IndexedLineReader reader3 = new IndexedLineReader("src/test/resources/readLineTestFile3.txt");

        assertEquals("line 1", reader1.readLine(0));
        assertEquals("line 1", reader2.readLine(0));
        assertEquals("line 1", reader3.readLine(0));
        assertEquals("line 2", reader1.readLine(1));
        assertEquals("line 2", reader2.readLine(1));
        assertEquals("line 2", reader3.readLine(1));
        assertEquals("line 3", reader1.readLine(2));
        assertEquals("line 3", reader2.readLine(2));
        assertEquals("line 3", reader3.readLine(2));
        assertEquals("line 4", reader1.readLine(3));
        assertEquals("line 4", reader2.readLine(3));
        assertEquals("line 4", reader3.readLine(3));
    }

    @Test
    public void testIndexingWithCustomSeparator() throws IOException {
        IndexedLineReader reader1 = new IndexedLineReader("src/test/resources/utils/indexingtestfile1.txt", "#####");

        assertEquals("line 1\r\n", reader1.readLine(0));
        assertEquals("\r\nline 2\r\n", reader1.readLine(1));

        IndexedLineReader reader2 = new IndexedLineReader("src/test/resources/utils/indexingtestfile2.txt", "#####");
        assertEquals("\r\nline 0\r\n", reader2.readLine(0));
        assertEquals("line 1\r\n", reader2.readLine(1));
        assertEquals("\r\nline 2\r\n", reader2.readLine(2));
    }
}
