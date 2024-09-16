package org.aksw.iguana.cc.lang.impl;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class SaxSparqlJsonResultCountingParserTest {

    private final String SELECT_RESULT_WITHOUT_LINKS = """
            {
              "head": { "vars": [ "book" , "title" ]
              } ,
              "results": {
                "bindings": [
                  {
                    "book": { "type": "uri" , "value": "http://example.org/book/book6" } ,
                    "title": { "type": "literal" , "value": "Harry Potter and the Half-Blood Prince" }
                  } ,
                  {
                    "book": { "type": "uri" , "value": "http://example.org/book/book7" } ,
                    "title": { "type": "literal" , "value": "Harry Potter and the Deathly Hallows" }
                  } ,
                  {
                    "book": { "type": "uri" , "value": "http://example.org/book/book5" } ,
                    "title": { "type": "literal" , "value": "Harry Potter and the Order of the Phoenix" }
                  } ,
                  {
                    "book": { "type": "uri" , "value": "http://example.org/book/book4" } ,
                    "title": { "type": "literal" , "value": "Harry Potter and the Goblet of Fire" }
                  } ,
                  {
                    "book": { "type": "uri" , "value": "http://example.org/book/book2" } ,
                    "title": { "type": "literal" , "value": "Harry Potter and the Chamber of Secrets" }
                  } ,
                  {
                    "book": { "type": "uri" , "value": "http://example.org/book/book3" } ,
                    "title": { "type": "literal" , "value": "Harry Potter and the Prisoner Of Azkaban" }
                  } ,
                  {
                    "book": { "type": "uri" , "value": "http://example.org/book/book1" } ,
                    "title": { "type": "literal" , "value": "Harry Potter and the Philosopher's Stone" }
                  }
                ]
              }
            }
            """; // 7 results, 14 bindings

    private final String SELECT_RESULT = """
            {
               "head": {
                   "link": ["http://www.w3.org/TR/rdf-sparql-XMLres/example.rq"],
                   "vars": [
                       "x",
                       "hpage",
                       "name",
                       "mbox",
                       "age",
                       "blurb",
                       "friend"
                       ]
                   },
               "results": {
                   "bindings": [
                           {
                               "x" : { "type": "bnode", "value": "r1" },
                               "hpage" : { "type": "uri", "value": "http://work.example.org/alice/" },
                               "name" : {  "type": "literal", "value": "Alice" } ,
            		           "mbox" : {  "type": "literal", "value": "" } ,
                               "blurb" : {
                                 "datatype": "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral",
                                 "type": "literal",
                                 "value": "<p xmlns=\\"http://www.w3.org/1999/xhtml\\">My name is <b>alice</b></p>"
                               },
                               "friend" : { "type": "bnode", "value": "r2" }
                           },
                           {
                               "x" : { "type": "bnode", "value": "r2" },
                               "hpage" : { "type": "uri", "value": "http://work.example.org/bob/" },
                               "name" : { "type": "literal", "value": "Bob", "xml:lang": "en" },
                               "mbox" : { "type": "uri", "value": "mailto:bob@work.example.org" },
                               "friend" : { "type": "bnode", "value": "r1" }
                           }
                       ]
                   }
            }
           """; // 2 results, 11 bindings

    private final String ASK_RESULT_WIHTOUT_LINKS = """
            {
              "head": { } ,
              "boolean": true
            }
            """;

    private final String ASK_RESULT = """
            {
              "head": { "link": ["http://www.w3.org/TR/rdf-sparql-XMLres/example.rq"] } ,
              "boolean": true
            }
            """;

    @Test
    public void testSelectWithLink() {
        final SaxSparqlJsonResultCountingParser parser = new SaxSparqlJsonResultCountingParser();
        final var data = parser.process(new ByteArrayInputStream(SELECT_RESULT.getBytes()), 0);
        assertInstanceOf(ResultCountData.class, data);
        final var result = (ResultCountData) data;
        assertEquals(2, result.results());
        assertEquals(11, result.bindings());
        assertEquals(7, result.variables().size());
        assertEquals(1, result.links().size());
        assertEquals("x", result.variables().get(0));
        assertEquals("hpage", result.variables().get(1));
        assertEquals("name", result.variables().get(2));
        assertEquals("mbox", result.variables().get(3));
        assertEquals("age", result.variables().get(4));
        assertEquals("blurb", result.variables().get(5));
        assertEquals("friend", result.variables().get(6));
        assertEquals("http://www.w3.org/TR/rdf-sparql-XMLres/example.rq", result.links().get(0));
    }

    @Test
    public void testSelectWithoutLink() {
        final SaxSparqlJsonResultCountingParser parser = new SaxSparqlJsonResultCountingParser();
        final var data = parser.process(new ByteArrayInputStream(SELECT_RESULT_WITHOUT_LINKS.getBytes()), 0);
        assertInstanceOf(ResultCountData.class, data);
        final var result = (ResultCountData) data;
        assertEquals(7, result.results());
        assertEquals(14, result.bindings());
        assertEquals(2, result.variables().size());
        assertEquals(0, result.links().size());
        assertEquals("book", result.variables().get(0));
        assertEquals("title", result.variables().get(1));
    }

    @Test
    public void testAskWithLink() {
        final SaxSparqlJsonResultCountingParser parser = new SaxSparqlJsonResultCountingParser();
        final var data = parser.process(new ByteArrayInputStream(ASK_RESULT.getBytes()), 0);
        assertInstanceOf(BooleanResultData.class, data);
        final var result = (BooleanResultData) data;
        assertTrue(result.result());
        assertEquals("http://www.w3.org/TR/rdf-sparql-XMLres/example.rq", result.links().get(0));
    }

    @Test
    public void testAskWithoutLink() {
        final SaxSparqlJsonResultCountingParser parser = new SaxSparqlJsonResultCountingParser();
        final var data = parser.process(new ByteArrayInputStream(ASK_RESULT_WIHTOUT_LINKS.getBytes()), 0);
        assertInstanceOf(BooleanResultData.class, data);
        final var result = (BooleanResultData) data;
        assertTrue(result.result());
        assertEquals(0, result.links().size());
    }
}