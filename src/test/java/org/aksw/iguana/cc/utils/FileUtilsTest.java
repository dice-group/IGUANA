package org.aksw.iguana.cc.utils;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileUtilsTest {

    @Test
    public void countLinesTest() throws IOException {
        //get test file
        File f = new File("src/test/resources/fileUtils.txt");
        //count lines
        assertEquals(6, FileUtils.countLines(f));
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
