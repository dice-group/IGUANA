package org.aksw.iguana.cc.lang;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.jena.rdf.model.Model;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Language Processor tells how to handle Http responses as well as how to analyze queries and generate stats.
 */
public interface LanguageProcessor {

    /**
     * Returns the prefix used for the queries (e.g. sparql, query or document)
     * @return
     */
    String getQueryPrefix();

    /**
     * Method to generate Triple Statistics for provided queries
     *
     *
     * @param taskID
     * @return Model with the triples to add to the results
     */
    Model generateTripleStats(List<QueryWrapper> queries, String resourcePrefix, String taskID);


    /**
     * Gets the result size of a given HTTP response
     *
     * @param response
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseException
     * @throws IOException
     */
    Long getResultSize(CloseableHttpResponse response) throws ParserConfigurationException, SAXException, ParseException, IOException;

    Long getResultSize(Header contentTypeHeader, ByteArrayOutputStream content, long contentLength) throws ParserConfigurationException, SAXException, ParseException, IOException;


    long readResponse(InputStream inputStream, ByteArrayOutputStream responseBody) throws IOException, TimeoutException;

}
