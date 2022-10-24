package org.aksw.iguana.cc.query.source;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class FileSeparatorQuerySourceTest {

    private final FileSeparatorQuerySource querySource;

    public FileSeparatorQuerySourceTest(String path, String separator) {
        if (separator == null) {
            this.querySource = new FileSeparatorQuerySource(path);
        } else {
            this.querySource = new FileSeparatorQuerySource(path, separator);
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