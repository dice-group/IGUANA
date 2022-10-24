package org.aksw.iguana.cc.query.source;

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
    public void size() {
        assertEquals(3, this.querySource.size());
    }

    @Test
    public void getQuery() throws IOException {
        assertEquals("QUERY 1 {still query 1}", this.querySource.getQuery(0));
        assertEquals("QUERY 2 {still query 2}", this.querySource.getQuery(1));
        assertEquals("QUERY 3 {still query 3}", this.querySource.getQuery(2));
    }

    @Test
    public void getAllQueries() throws IOException {
        List<String> expected = new ArrayList<>(3);
        expected.add("QUERY 1 {still query 1}");
        expected.add("QUERY 2 {still query 2}");
        expected.add("QUERY 3 {still query 3}");

        assertEquals(expected, this.querySource.getAllQueries());
    }
}