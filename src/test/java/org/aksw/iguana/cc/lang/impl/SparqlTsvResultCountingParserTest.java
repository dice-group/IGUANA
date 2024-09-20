package org.aksw.iguana.cc.lang.impl;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class SparqlTsvResultCountingParserTest {

    private static final String TEST_STRING = """
            ?x\t?literal
            <http://example/x>\t"String"
            <http://example/x>\t"String-with-dquote\\""
            _:blank0\t"Blank node"
            \t"Missing 'x'"
            \t
            <http://example/x>\t
            _:blank1\t"String-with-lang"@en
            _:blank1\t123
            """;

    @Test
    public void test() {
        final SparqlTsvResultCountingParser parser = new SparqlTsvResultCountingParser();
        final var data = parser.process(new ByteArrayInputStream(TEST_STRING.getBytes()), 0);
        assertInstanceOf(ResultCountData.class, data);
        final var result = (ResultCountData) data;
        assertEquals(8, result.results());
        assertEquals(12, result.bindings());
        assertEquals(2, result.variables().size());
        assertEquals("?x", result.variables().get(0));
        assertEquals("?literal", result.variables().get(1));
    }
}