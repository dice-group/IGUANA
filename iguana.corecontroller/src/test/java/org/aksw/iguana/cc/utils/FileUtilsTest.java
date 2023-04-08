package org.aksw.iguana.cc.utils;

import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileUtilsTest {

    @Test
    public void countLinesTest() throws IOException {
        //get test file
        File f = new File("src/test/resources/fileUtils.txt");
        //count lines

        long startTime = 0;
        long endTime = 0;

        startTime = System.nanoTime();
        assertEquals(6, FileUtils.countLines(f));
        endTime = System.nanoTime();
        System.out.println(((double) (endTime - startTime) / 1000000) + "ms");

        File f1 = new File("src/test/resources/readLineTestFile1.txt");
        File f2 = new File("src/test/resources/readLineTestFile2.txt");
        File f3 = new File("src/test/resources/readLineTestFile3.txt");
        startTime = System.nanoTime();
        assertEquals(4, FileUtils.countLines(f1));
        endTime = System.nanoTime();
        System.out.println(((double) (endTime - startTime) / 1000000) + "ms");
        startTime = System.nanoTime();
        assertEquals(4, FileUtils.countLines(f2));
        endTime = System.nanoTime();
        System.out.println(((double) (endTime - startTime) / 1000000) + "ms");
        startTime = System.nanoTime();
        assertEquals(4, FileUtils.countLines(f3));
        endTime = System.nanoTime();
        System.out.println(((double) (endTime - startTime) / 1000000) + "ms");
    }

    @Test
    public void readLineAtTest() throws IOException {
        //get test file
        File f = new File("src/test/resources/fileUtils.txt");
        //read line at 2, 15
        assertEquals("a", FileUtils.readLineAt(0, f));
        assertEquals("abc", FileUtils.readLineAt(2, f));
        //is at actual line 16, but as all the lines between line 4-10 and 12-15 are empty this should be the 4th
        assertEquals("dfe", FileUtils.readLineAt(4, f));
        //read line at -1
        assertEquals("", FileUtils.readLineAt(-1, f));

        File f1 = new File("src/test/resources/readLineTestFile1.txt");
        File f2 = new File("src/test/resources/readLineTestFile2.txt");
        File f3 = new File("src/test/resources/readLineTestFile3.txt");
        assertEquals("line 1", FileUtils.readLineAt(0, f1));
        assertEquals("line 1", FileUtils.readLineAt(0, f2));
        assertEquals("line 1", FileUtils.readLineAt(0, f3));
        assertEquals("line 2", FileUtils.readLineAt(1, f1));
        assertEquals("line 2", FileUtils.readLineAt(1, f2));
        assertEquals("line 2", FileUtils.readLineAt(1, f3));
        assertEquals("line 3", FileUtils.readLineAt(2, f1));
        assertEquals("line 3", FileUtils.readLineAt(2, f2));
        assertEquals("line 3", FileUtils.readLineAt(2, f3));
        assertEquals("line 4", FileUtils.readLineAt(3, f1));
        assertEquals("line 4", FileUtils.readLineAt(3, f2));
        assertEquals("line 4", FileUtils.readLineAt(3, f3));
    }

    @Test
    public void readTest() throws IOException {
        //read whole content
        String data = FileUtils.readFile("src/test/resources/fileUtils.txt");
        String expected = "a\nab\nabc\n\n\n\n\n\n\n\n\\n\n\n\n\n\ndfe\n\ntest";
        assertEquals(expected, data);
    }

    @Test
    public void getHashTest(){
        //check if hash abs works
        assertTrue(FileUtils.getHashcodeFromFileContent("src/test/resources/fileUtils.txt")>0);
    }
}
