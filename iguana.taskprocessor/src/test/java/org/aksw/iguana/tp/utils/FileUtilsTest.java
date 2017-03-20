package org.aksw.iguana.tp.utils;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


/**
 * This will test if the FileUtils method to calculate the file size works as expected.
 * @author f.conrads
 *
 */
@RunWith(Parameterized.class)
public class FileUtilsTest {

    private String file;
    private int length;
    private String expectedLine;
    private int pos;
    
    /**
     * @return Configurations to test
     */
    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> testConfigs = new ArrayList<Object[]>();
        testConfigs.add(new Object[]{"src/test/resources/emptyFile.txt", 0, -1, ""});
        testConfigs.add(new Object[]{"src/test/resources/fullFile.txt", 5, 1, "asd2"});
        testConfigs.add(new Object[]{"src/test/resources/emptyLinesFile.txt", 5, 1, "asd2"});
        testConfigs.add(new Object[]{"src/test/resources/onlyOneLineFile.txt", 1, 0, "asd"});
        return testConfigs;
    }
    
    /**
     * Constructor for the FileUtils Test
     * 
     * @param file file to test
     * @param length expected File Length
     * @param pos position of the line
     * @param expectedLine line which should be returned
     */
    public FileUtilsTest(String file, int length, int pos, String expectedLine) {
	this.file = file;
	this.length = length;
	this.pos = pos;
	this.expectedLine=expectedLine;
    }
    
    /**
     * Tests if the current File matches the length calculated by the FileUtils methods
     * 
     * @throws IOException 
     */
    @Test
    public void testSize() throws IOException{
	assertEquals(this.length, FileUtils.countLines(new File(file)));
	assertEquals(this.length, FileUtils.countLines(new File(file)));
    }
    
    /**
     * Tests if the FileUtils get the correct Line
     * 
     * @throws IOException 
     * 
     */
    @Test 
    public void testPos() throws IOException{
	if(this.pos != -1){
	    assertEquals(this.expectedLine, FileUtils.readLineAt(this.pos, new File(file)));
	}
    }

    
}
