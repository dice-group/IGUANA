package org.aksw.iguana.cc.utils;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static java.nio.file.Files.createTempFile;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class FileUtilsTest {

    @RunWith(Parameterized.class)
    public static class TestGetLineEnding {
        private static class TestData {
            public Path file;
            public String expectedLineEnding;

            public TestData(String expectedLineEnding) {
                this.expectedLineEnding = expectedLineEnding;


            }
        }

        public TestGetLineEnding(String expectedLineEnding) throws IOException {
            this.data = new TestData(expectedLineEnding);
            this.data.file = createTempFile("TestGetLineEnding", ".txt");
            this.data.file.toFile().deleteOnExit();
            writeStringToFile(this.data.file.toFile(), "a" + this.data.expectedLineEnding + "b" + this.data.expectedLineEnding, StandardCharsets.UTF_8);
        }

        private final TestData data;

        @Parameterized.Parameters
        public static Collection<String> data() {
            return List.of(
                    "\n", /* unix */
                    "\r", /* old mac */
                    "\r\n" /* windows */
            );
        }

        @Test
        public void testGetLineEndings() throws IOException {
            assertEquals(FileUtils.getLineEnding(this.data.file.toString()), this.data.expectedLineEnding);
        }
    }

    @RunWith(Parameterized.class)
    public static class TestIndexStream {

        private final TestData data;

        public TestIndexStream(TestData data) {
            this.data = data;
        }

        public static class TestData {
            /**
             * String to be separated
             */
            String string;
            /**
             * Separating sequence
             */
            String separator;

            /**
             * List of [offset, length] arrays
             */
            List<long[]> index;

            public TestData(String string, String separator, List<long[]> index) {
                this.string = string;
                this.separator = separator;
                this.index = index;
            }
        }
        @Parameterized.Parameters
        public static Collection<TestData> data() {


            return List.of(
                    new TestData("", "a", Arrays.asList(new long[]{0, 0})),
                    new TestData("a", "a", Arrays.asList(new long[]{0, 0}, new long[]{1, 0})),
                    new TestData("abc", "b", Arrays.asList(new long[]{0, 1}, new long[]{2, 1})),
                    new TestData("1\n2", "\n", Arrays.asList(new long[]{0, 1}, new long[]{2, 1})),
                    new TestData("1\t2", "\t", Arrays.asList(new long[]{0, 1}, new long[]{2, 1})),
                    new TestData("abcbd", "b", Arrays.asList(new long[]{0, 1}, new long[]{2, 1}, new long[]{4, 1})),
                    new TestData("aab", "ab", Arrays.asList(new long[]{0, 1}, new long[]{3, 0})),
                    new TestData("aaaabaabaa", "ab", Arrays.asList(new long[]{0, 3}, new long[]{5, 1}, new long[]{8, 2})),
                    new TestData("1\n\t\n2", "\n\t\n", Arrays.asList(new long[]{0, 1}, new long[]{4, 1}))

            );
        }

        @Test
        public void testIndexingStrings() throws IOException {
            //check if hash abs works

            List<long[]> index = FileUtils.indexStream(data.separator, new ByteArrayInputStream(data.string.getBytes()));

            assertEquals(data.index.size(), index.size());
            for (int i = 0; i < index.size(); i++) {
                assertArrayEquals(data.index.get(i), index.get(i));
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class ParameterizedTest {
        private final Path file;

        private final String content;

        public ParameterizedTest(String content) throws IOException {
            this.file = createTempFile("getHashTest", ".txt");
            writeStringToFile(this.file.toFile(), content,  StandardCharsets.UTF_8);
            this.file.toFile().deleteOnExit();

            this.content = content;
        }

        @Parameterized.Parameters
        public static Collection<String> data() {

            return Arrays.asList(
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString()
            );
        }

        @Test
        public void getHashTest(){
            //check if hash abs works
            final int expected = Math.abs(content.hashCode());
            final int actual = FileUtils.getHashcodeFromFileContent(this.file.toString());
            assertTrue(actual >= 0);
            assertEquals(expected, actual);
        }
    }

    public static class NonParameterizedTest {
        @Test
        public void readTest() throws IOException {

            Path file = createTempFile("readTest", ".txt");
            file.toFile().deleteOnExit();
            String expectedString = UUID.randomUUID() + "\n\t\r" + UUID.randomUUID() + "\n";
            writeStringToFile(file.toFile(), expectedString,  StandardCharsets.UTF_8);

            //read whole content
            String actualString = FileUtils.readFile(file.toString());

            assertEquals(expectedString, actualString);
        }
    }
}
