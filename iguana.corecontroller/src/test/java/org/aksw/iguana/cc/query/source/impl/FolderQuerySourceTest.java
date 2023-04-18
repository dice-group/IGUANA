package org.aksw.iguana.cc.query.source.impl;

import org.aksw.iguana.cc.utils.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    public void getAllQueriesTest() throws IOException {
        // The method used to index the folder doesn't ensure a fixed order of the queries
        HashSet<String> expected = new HashSet<>();
        expected.add("QUERY 1 {still query 1}");
        expected.add("QUERY 2 {still query 2}");
        expected.add("QUERY 3 {still query 3}");

        assertTrue(expected.containsAll(this.querySource.getAllQueries()));
    }

    @Test
    public void getHashcodeTest() {
        int expected = FileUtils.getHashcodeFromFileContent(querySource.files[0].getAbsolutePath());
        assertEquals(expected, this.querySource.hashCode());
        assertTrue(this.querySource.hashCode() > 0);
    }
}