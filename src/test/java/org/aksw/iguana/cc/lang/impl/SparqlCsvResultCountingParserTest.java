package org.aksw.iguana.cc.lang.impl;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class SparqlCsvResultCountingParserTest {

    private static final String TEST_STRING = """
            x,literal\r
            http://example/x,String\r
            http://example/x,"String-with-dquote""\"\r
            _:b0,Blank node\r
            ,Missing 'x'\r
            ,\r
            http://example/x,\r
            _:b1,String-with-lang\r
            _:b1,123\r
            """;

    @Test
    public void test() {
        final SparqlCsvResultCountingParser parser = new SparqlCsvResultCountingParser();
        final var data = parser.process(new ByteArrayInputStream(TEST_STRING.getBytes()), 0);
        assertInstanceOf(ResultCountData.class, data);
        final var result = (ResultCountData) data;
        assertEquals(8, result.results());
        assertEquals(12, result.bindings());
        assertEquals(2, result.variables().size());
        assertEquals("x", result.variables().get(0));
        assertEquals("literal", result.variables().get(1));
    }
}
