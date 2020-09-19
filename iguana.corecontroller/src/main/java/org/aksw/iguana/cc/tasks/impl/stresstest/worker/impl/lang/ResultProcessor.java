package org.aksw.iguana.cc.tasks.impl.stresstest.worker.impl.lang;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * ResultProcessor interface to implement for different languages.
 * Will determine how HTTP responses will be validated, read, parsed etc.
 */
public interface ResultProcessor {

    public Long getResultSize(CloseableHttpResponse response) throws ParserConfigurationException, SAXException, ParseException, IOException;
}
