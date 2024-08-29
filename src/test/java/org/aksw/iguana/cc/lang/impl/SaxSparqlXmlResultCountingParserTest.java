package org.aksw.iguana.cc.lang.impl;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class SaxSparqlXmlResultCountingParserTest {

    private final String SELECT_RESULT = """
    <?xml version="1.0"?>
    <sparql xmlns="http://www.w3.org/2005/sparql-results#">
    
      <head>
        <variable name="x"/>
        <variable name="hpage"/>
        <variable name="name"/>
        <variable name="age"/>
        <variable name="mbox"/>
        <variable name="friend"/>
      </head>
    
      <results>
        <result>
          <binding name="x"><bnode>r2</bnode></binding>
          <binding name="hpage"><uri>http://work.example.org/bob/</uri></binding>
          <binding name="name"><literal xml:lang="en">Bob</literal></binding>
          <binding name="age"><literal datatype="http://www.w3.org/2001/XMLSchema#integer">30</literal></binding>
          <binding name="mbox"><uri>mailto:bob@work.example.org</uri></binding>
        </result>
      </results>
    
    </sparql>
    """;

    @Test
    public void testSelectResult() {
        final var parser = new SaxSparqlXmlResultCountingParser();
        final var data = parser.process(new ByteArrayInputStream(SELECT_RESULT.getBytes()), 0);
        final var result = (SaxSparqlXmlResultCountingParser.SaxSparqlXmlResultData) data;
        assertEquals(1, result.results());
        assertEquals(5, result.bindings());
        assertEquals(6, result.variables().size());
        assertTrue(result.variables().contains("x"));
        assertTrue(result.variables().contains("hpage"));
        assertTrue(result.variables().contains("name"));
        assertTrue(result.variables().contains("age"));
        assertTrue(result.variables().contains("mbox"));
        assertTrue(result.variables().contains("friend"));
    }
}