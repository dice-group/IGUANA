package org.aksw.iguana.cc.utils;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class FileUtilsTest {

    @RunWith(Parameterized.class)
    public static class ParameterizedTest {
        private final File file;

        public ParameterizedTest(File file) {
            this.file = file;
        }

        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            File f1 = new File("src/test/resources/readLineTestFile1.txt");
            File f2 = new File("src/test/resources/readLineTestFile2.txt");
            File f3 = new File("src/test/resources/readLineTestFile3.txt");

            return Arrays.asList(new Object[][]{
                    {f1},
                    {f2},
                    {f3}
            });
        }

        @Test
        public void getHashTest(){
            //check if hash abs works
            assertTrue(FileUtils.getHashcodeFromFileContent(this.file.getAbsolutePath())>0);
        }
    }

    public static class NonParameterizedTest {
        @Test
        public void readTest() throws IOException {
            //read whole content
            String data = FileUtils.readFile("src/test/resources/fileUtils.txt");
            String expected = "a\nab\nabc\n\n\n\n\n\n\n\n\\n\n\n\n\n\ndfe\n\ntest";
            assertEquals(expected, data);
        }
    }
}
