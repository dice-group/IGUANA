package org.aksw.iguana.cc.lang;

import org.aksw.iguana.cc.lang.impl.SPARQLLanguageProcessor;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SPARQLLanguageProcessorTest {

    private String jsonResult = "{\n" +
            "  \"head\": { \"vars\": [ \"book\" , \"title\" ]\n" +
            "  } ,\n" +
            "  \"results\": { \n" +
            "    \"bindings\": [\n" +
            "      {\n" +
            "        \"book\": { \"type\": \"uri\" , \"value\": \"http://example.org/book/book3\" } ,\n" +
            "        \"title\": { \"type\": \"literal\" , \"value\": \"Example Book 3\" }\n" +
            "      } ,\n" +
            "      {\n" +
            "        \"book\": { \"type\": \"uri\" , \"value\": \"http://example.org/book/book2\" } ,\n" +
            "        \"title\": { \"type\": \"literal\" , \"value\": \"Example Book 2\" }\n" +
            "      } ,\n" +
            "      {\n" +
            "        \"book\": { \"type\": \"uri\" , \"value\": \"http://example.org/book/book1\" } ,\n" +
            "        \"title\": { \"type\": \"literal\" , \"value\": \"Example Book 1\" }\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";
    private String xmlResult = "<?xml version=\"1.0\"?>\n" +
            "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">\n" +
            "  <head>\n" +
            "    <variable name=\"x\"/>\n" +
            "    <variable name=\"hpage\"/>\n" +
            "  </head>\n" +
            "\n" +
            "  <results>\n" +
            "    <result>\n" +
            "      <binding name=\"x\">test1</binding>\n" +
            "      <binding name=\"hpage\"> ... </binding>\n" +
            "    </result>\n" +
            "\n" +
            "    <result>\n" +
            "      <binding name=\"x\">test2</binding>\n" +
            "      <binding name=\"hpage\"> ... </binding>\n" +
            "    </result>\n" +
            "    \n" +
            "  </results>\n" +
            "\n" +
            "</sparql>";




    @Test
    public void checkJSON() throws ParseException, IOException {
        ByteArrayOutputStream bbaos = new ByteArrayOutputStream();
        bbaos.write(jsonResult.getBytes());
        assertEquals(3, SPARQLLanguageProcessor.getJsonResultSize(bbaos));
        //test if valid json response provide 0 bindings
        try {
            //check if invalid json throws exception
            bbaos = new ByteArrayOutputStream();
            bbaos.write("{ \"a\": \"b\"}".getBytes());
            SPARQLLanguageProcessor.getJsonResultSize(bbaos);
            assertTrue("Should have thrown an error", false);
        }catch(Exception e){
            assertTrue(true);
        }
        try {
            //check if invalid json throws exception
            bbaos = new ByteArrayOutputStream();
            bbaos.write("{ \"a\": \"b\"".getBytes());
            SPARQLLanguageProcessor.getJsonResultSize(bbaos);
            assertTrue("Should have thrown an error", false);
        }catch(Exception e){
            assertTrue(true);
        }
    }

    @Test
    public void checkXML() throws IOException, SAXException, ParserConfigurationException {
        ByteArrayOutputStream bbaos = new ByteArrayOutputStream();
        bbaos.write(xmlResult.getBytes(StandardCharsets.UTF_8));
        assertEquals(2, SPARQLLanguageProcessor.getXmlResultSize(bbaos));
        //test if valid xml response provide 0 bindings
        try {
            //check if invalid xml throws exception
            bbaos = new ByteArrayOutputStream();
            bbaos.write("<a>b</a>".getBytes());
            SPARQLLanguageProcessor.getJsonResultSize(bbaos);
            assertTrue("Should have thrown an error", false);
        }catch(Exception e){
            assertTrue(true);
        }
        try {
            //check if invalid xml throws exception
            bbaos = new ByteArrayOutputStream();
            bbaos.write("{ \"a\": \"b\"".getBytes());
            SPARQLLanguageProcessor.getJsonResultSize(bbaos);
            assertTrue("Should have thrown an error", false);
        }catch(Exception e){
            assertTrue(true);
        }
    }

    @Test
    public void checkResultSize() throws IOException, ParserConfigurationException, SAXException, ParseException {
        SPARQLLanguageProcessor languageProcessor = new SPARQLLanguageProcessor();
        assertEquals(3, languageProcessor.getResultSize(MockCloseableHttpResponse.buildMockResponse(jsonResult, SPARQLLanguageProcessor.QUERY_RESULT_TYPE_JSON)).longValue());
        assertEquals(2, languageProcessor.getResultSize(MockCloseableHttpResponse.buildMockResponse(xmlResult, SPARQLLanguageProcessor.QUERY_RESULT_TYPE_XML)).longValue());
        assertEquals(4, languageProcessor.getResultSize(MockCloseableHttpResponse.buildMockResponse("a\na\na\nb", "text/plain")).longValue());
    }


    @Test
    public void checkGeneratedStatsModel() throws IOException {
        Query q = QueryFactory.create("SELECT * {?s ?p ?o. ?o ?q ?t. FILTER(?t = \"abc\")} GROUP BY ?s");
        QueryWrapper wrapped = new QueryWrapper(q, "5");
        SPARQLLanguageProcessor languageProcessor = new SPARQLLanguageProcessor();
        Model actual = languageProcessor.generateTripleStats(Lists.newArrayList(wrapped),"query","1/1/2");
        Model expected = ModelFactory.createDefaultModel();
        expected.read(new FileReader("src/test/resources/querystats.nt"), null, "N-TRIPLE");
        assertEquals(expected.size(), actual.size());
        expected.remove(actual);
        actual.write(new FileWriter("test2.nt"),  "N-TRIPLE");
        assertEquals(0, expected.size());
    }
}
