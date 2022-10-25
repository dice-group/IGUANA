package org.aksw.iguana.cc.query.source.impl;

import org.aksw.iguana.cc.utils.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FileLineQuerySourceTest {

    private static final String PATH = "src/test/resources/query/source/queries.txt";

    private final FileLineQuerySource querySource;

    public FileLineQuerySourceTest() throws IOException {
        this.querySource = new FileLineQuerySource(PATH);
    }

    @Test
    public void sizeTest() {
        assertEquals(3, this.querySource.size());
    }

    @Test
    public void getQueryTest() throws IOException {
        assertEquals("QUERY 1 {still query 1}", this.querySource.getQuery(0));
        assertEquals("QUERY 2 {still query 2}", this.querySource.getQuery(1));
        assertEquals("QUERY 3 {still query 3}", this.querySource.getQuery(2));
    }

    @Test
    public void getAllQueriesTest() throws IOException {
        List<String> expected = new ArrayList<>(3);
        expected.add("QUERY 1 {still query 1}");
        expected.add("QUERY 2 {still query 2}");
        expected.add("QUERY 3 {still query 3}");

        assertEquals(expected, this.querySource.getAllQueries());
    }

    @Test
    public void getHashcodeTest() {
        int expected = FileUtils.getHashcodeFromFileContent(PATH);
        assertEquals(expected, this.querySource.getHashcode());
    }
}