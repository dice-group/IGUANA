package org.aksw.iguana.cc.lang;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.streams.Streams;
import org.aksw.iguana.rp.vocab.Vocab;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

public abstract class AbstractLanguageProcessor implements LanguageProcessor {

    @Override
    public String getQueryPrefix() {
        return "query";
    }

    @Override
    public Model generateTripleStats(List<QueryWrapper> queries, String resourcePrefix, String taskID) {
        Model model = ModelFactory.createDefaultModel();
        for(QueryWrapper wrappedQuery : queries) {
            Resource subject = ResourceFactory.createResource(COMMON.RES_BASE_URI + resourcePrefix + "/" + wrappedQuery.getId());
            model.add(subject, RDF.type, Vocab.queryClass);
            model.add(subject, Vocab.queryIDProp, ResourceFactory.createTypedLiteral(wrappedQuery.getId()));
            model.add(subject, RDFS.label, wrappedQuery.getQuery().toString());
        }
        return model;
    }

    @Override
    public Long getResultSize(CloseableHttpResponse response) throws ParserConfigurationException, SAXException, ParseException, IOException {
        return response.getEntity().getContentLength();
    }

    @Override
    public Long getResultSize(Header contentTypeHeader, ByteArrayOutputStream content, long contentLength) throws ParserConfigurationException, SAXException, ParseException, IOException {
        return Long.valueOf(content.size());
    }

    @Override
    public long readResponse(InputStream inputStream, ByteArrayOutputStream responseBody) throws IOException, TimeoutException {
        return Streams.inputStream2ByteArrayOutputStream(inputStream, responseBody);
    }

    //@Override
    public long readResponse(InputStream inputStream, Instant startTime, Double timeOut, ByteArrayOutputStream responseBody) throws IOException, TimeoutException {
        return Streams.inputStream2ByteArrayOutputStream(inputStream, startTime, timeOut, responseBody);
    }
}
