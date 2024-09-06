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
        <link href="http://www.w3.org/TR/rdf-sparql-XMLres" />
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

    private final String SELECT_RESULT_WITHOUT_LINK = """
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

    private final String ASK_RESULT = """
    <?xml version="1.0"?>
    <sparql xmlns="http://www.w3.org/2005/sparql-results#">
    
      <head>
        <link href="http://www.w3.org/TR/rdf-sparql-XMLres" />
      </head>
    
      <boolean>true</boolean>
    </sparql>
    """;

    private final String ASK_RESULT_WITHOUT_LINK = """
    <?xml version="1.0"?>
    <sparql xmlns="http://www.w3.org/2005/sparql-results#">
    
      <head>
      </head>
    
      <boolean>true</boolean>
    </sparql>
    """;

    @Test
    public void testSelectResult() {
        final var parser = new SaxSparqlXmlResultCountingParser();
        final var data = parser.process(new ByteArrayInputStream(SELECT_RESULT.getBytes()), 0);
        final var result = (ResultCountData) data;
        assertEquals(1, result.results());
        assertEquals(5, result.bindings());
        assertEquals(6, result.variables().size());
        assertEquals(1, result.links().size());
        assertTrue(result.variables().contains("x"));
        assertTrue(result.variables().contains("hpage"));
        assertTrue(result.variables().contains("name"));
        assertTrue(result.variables().contains("age"));
        assertTrue(result.variables().contains("mbox"));
        assertTrue(result.variables().contains("friend"));
        assertTrue(result.links().contains("http://www.w3.org/TR/rdf-sparql-XMLres"));
    }

    @Test
    public void testSelectResultWithoutLink() {
        final var parser = new SaxSparqlXmlResultCountingParser();
        final var data = parser.process(new ByteArrayInputStream(SELECT_RESULT_WITHOUT_LINK.getBytes()), 0);
        final var result = (ResultCountData) data;
        assertEquals(1, result.results());
        assertEquals(5, result.bindings());
        assertEquals(6, result.variables().size());
        assertTrue(result.variables().contains("x"));
        assertTrue(result.variables().contains("hpage"));
        assertTrue(result.variables().contains("name"));
        assertTrue(result.variables().contains("age"));
        assertTrue(result.variables().contains("mbox"));
        assertTrue(result.variables().contains("friend"));
        assertTrue(result.links().isEmpty());
    }

    @Test
    public void testAskResult() {
        final var parser = new SaxSparqlXmlResultCountingParser();
        final var data = parser.process(new ByteArrayInputStream(ASK_RESULT.getBytes()), 0);
        assertInstanceOf(BooleanResultData.class, data);
        final var result = (BooleanResultData) data;
        assertTrue(result.result());
        assertTrue(result.links().contains("http://www.w3.org/TR/rdf-sparql-XMLres"));
        assertEquals(1, result.links().size());
    }

    @Test
    public void testAskResultWithoutLink() {
        final var parser = new SaxSparqlXmlResultCountingParser();
        final var data = parser.process(new ByteArrayInputStream(ASK_RESULT_WITHOUT_LINK.getBytes()), 0);
        assertInstanceOf(BooleanResultData.class, data);
        final var result = (BooleanResultData) data;
        assertTrue(result.result());
        assertTrue(result.links().isEmpty());
    }
}