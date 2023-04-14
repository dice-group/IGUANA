package org.aksw.iguana.cc.query.source.impl;

import org.aksw.iguana.cc.utils.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FolderQuerySourceTest {

    private static final String PATH = "src/test/resources/query/source/query-folder";

    private final FolderQuerySource querySource;

    public FolderQuerySourceTest() {
        this.querySource = new FolderQuerySource(PATH);
    }

    @Test
    public void sizeTest() {
        assertEquals(3, this.querySource.size());
    }

    @Test
    public void getQueryTest() throws IOException {
        assertEquals("QUERY 1 {\r\nstill query 1\r\n}", this.querySource.getQuery(0));
        assertEquals("QUERY 2 {\r\nstill query 2\r\n}", this.querySource.getQuery(1));
        assertEquals("QUERY 3 {\r\nstill query 3\r\n}", this.querySource.getQuery(2));
    }

    @Test
    public void getAllQueriesTest() throws IOException {
        List<String> expected = new ArrayList<>(3);
        expected.add("QUERY 1 {\r\nstill query 1\r\n}");
        expected.add("QUERY 2 {\r\nstill query 2\r\n}");
        expected.add("QUERY 3 {\r\nstill query 3\r\n}");

        assertEquals(expected, this.querySource.getAllQueries());
    }

    @Test
    public void getHashcodeTest() {
        int expected = FileUtils.getHashcodeFromFileContent(PATH + "/query1.txt");
        assertEquals(expected, this.querySource.hashCode());
    }
}