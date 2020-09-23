package org.aksw.iguana.cc.lang;

import com.google.common.collect.Lists;
import org.aksw.iguana.cc.lang.impl.SPARQLLanguageProcessor;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicStatusLine;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;

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


    public CloseableHttpResponse buildMockResponse(String data, String contentType) throws FileNotFoundException, UnsupportedEncodingException {
        ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
        String reasonPhrase = "OK";
        StatusLine statusline = new BasicStatusLine(protocolVersion, HttpStatus.SC_OK, reasonPhrase);
        MockCloseableHttpResponse mockResponse = new MockCloseableHttpResponse(statusline);
        BasicHttpEntity entity = new BasicHttpEntity();
        mockResponse.setHeader("Content-Type", contentType);
        //entity.setContentType(contentType);
        URL url = Thread.currentThread().getContextClassLoader().getResource("response.txt");
        InputStream instream = new ByteArrayInputStream(data.getBytes());
        entity.setContent(instream);
        mockResponse.setEntity(entity);
        return mockResponse;
    }

    @Test
    public void checkJSON() throws ParseException {
        assertEquals(3, SPARQLLanguageProcessor.getJsonResultSize(jsonResult));
        //test if valid json response provide 0 bindings
        try {
            //check if invalid json throws exception
            SPARQLLanguageProcessor.getJsonResultSize("{ \"a\": \"b\"}");
            assertTrue("Should have thrown an error", false);
        }catch(Exception e){
            assertTrue(true);
        }
        try {
            //check if invalid json throws exception
            SPARQLLanguageProcessor.getJsonResultSize("{ \"a\": \"b\"");
            assertTrue("Should have thrown an error", false);
        }catch(Exception e){
            assertTrue(true);
        }
    }

    @Test
    public void checkXML() throws IOException, SAXException, ParserConfigurationException {
        assertEquals(2, SPARQLLanguageProcessor.getXmlResultSize(xmlResult));
        //test if valid xml response provide 0 bindings
        try {
            //check if invalid xml throws exception
            SPARQLLanguageProcessor.getJsonResultSize("<a>b</a>");
            assertTrue("Should have thrown an error", false);
        }catch(Exception e){
            assertTrue(true);
        }
        try {
            //check if invalid xml throws exception
            SPARQLLanguageProcessor.getJsonResultSize("{ \"a\": \"b\"");
            assertTrue("Should have thrown an error", false);
        }catch(Exception e){
            assertTrue(true);
        }
    }

    @Test
    public void checkResultSize() throws IOException, ParserConfigurationException, SAXException, ParseException {
        SPARQLLanguageProcessor languageProcessor = new SPARQLLanguageProcessor();
        assertEquals(3, languageProcessor.getResultSize(buildMockResponse(jsonResult, SPARQLLanguageProcessor.QUERY_RESULT_TYPE_JSON)).longValue());
        assertEquals(2, languageProcessor.getResultSize(buildMockResponse(xmlResult, SPARQLLanguageProcessor.QUERY_RESULT_TYPE_XML)).longValue());
        assertEquals(4, languageProcessor.getResultSize(buildMockResponse("a\na\na\nb", "text/plain")).longValue());
    }


    @Test
    public void checkGeneratedStatsModel() throws IOException {
        Query q = QueryFactory.create("SELECT * {?s ?p ?o. ?o ?q ?t. FILTER(?t = \"abc\")} GROUP BY ?s");
        QueryWrapper wrapped = new QueryWrapper(q, "abc");
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
