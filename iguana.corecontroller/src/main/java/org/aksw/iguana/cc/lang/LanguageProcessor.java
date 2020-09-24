package org.aksw.iguana.cc.lang;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.jena.rdf.model.Model;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

public interface LanguageProcessor {

    public String getQueryPrefix();

    /**
     * Method to generate Triple Statistics for provided queries
     *
     *
     * @param taskID
     * @return Model with the triples to add to the results
     */
    public Model generateTripleStats(List<QueryWrapper> queries, String resourcePrefix, String taskID);


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
    public Long getResultSize(CloseableHttpResponse response) throws ParserConfigurationException, SAXException, ParseException, IOException;


}
