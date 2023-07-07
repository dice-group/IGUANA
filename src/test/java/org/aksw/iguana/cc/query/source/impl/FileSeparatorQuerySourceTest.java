package org.aksw.iguana.cc.query.source.impl;

import org.aksw.iguana.cc.utils.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class FileSeparatorQuerySourceTest {

    private final FileSeparatorQuerySource querySource;

    private final Path path;

    public FileSeparatorQuerySourceTest(String path, String separator) throws IOException {
        this.path = Paths.get(path);

        if (separator == null) {
            this.querySource = new FileSeparatorQuerySource(this.path);
        } else {
            this.querySource = new FileSeparatorQuerySource(this.path, separator);
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> testData = new ArrayList<>();
        testData.add(new Object[]{"src/test/resources/query/source/separated-queries-default.txt", null});
        testData.add(new Object[]{"src/test/resources/query/source/separated-queries-space.txt", ""});

        return testData;
    }

    @Test
    public void sizeTest() {
        assertEquals(3, this.querySource.size());
    }

    @Test
    public void getQueryTest() throws IOException {
        String le = FileUtils.getLineEnding(this.path);
        assertEquals("QUERY 1 {" + le + "still query 1" + le + "}", this.querySource.getQuery(0));
        assertEquals("QUERY 2 {" + le + "still query 2" + le + "}", this.querySource.getQuery(1));
        assertEquals("QUERY 3 {" + le + "still query 3" + le + "}", this.querySource.getQuery(2));
    }

    @Test
    public void getAllQueriesTest() throws IOException {
        List<String> expected = new ArrayList<>(3);
        String le = FileUtils.getLineEnding(this.path);
        expected.add("QUERY 1 {" + le + "still query 1" + le + "}");
        expected.add("QUERY 2 {" + le + "still query 2" + le + "}");
        expected.add("QUERY 3 {" + le + "still query 3" + le + "}");

        assertEquals(expected, this.querySource.getAllQueries());
    }

    @Test
    public void getHashcodeTest() {
        int expected = FileUtils.getHashcodeFromFileContent(this.path);
        assertEquals(expected, this.querySource.hashCode());
    }
}