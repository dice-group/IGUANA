package org.aksw.iguana.cc.lang.impl;

import org.aksw.iguana.cc.lang.AbstractLanguageProcessor;
import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.lang.QueryWrapper;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.vocab.Vocab;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Language for everything which returns RDF in any rdf format.
 *
 * Counts triples returned as ResultSize
 */
@Shorthand("lang.RDF")
public class RDFLanguageProcessor extends AbstractLanguageProcessor implements LanguageProcessor {

    private static Logger LOGGER = LoggerFactory.getLogger(RDFLanguageProcessor.class);
    protected String queryPrefix="document";

    @Override
    public String getQueryPrefix() {
        return this.queryPrefix;
    }

    @Override
    public Model generateTripleStats(List<QueryWrapper> queries, String resourcePrefix, String taskID) {
        Model model = ModelFactory.createDefaultModel();
        for(QueryWrapper wrappedQuery : queries) {
            Resource subject = ResourceFactory.createResource(COMMON.RES_BASE_URI + resourcePrefix + "/" + wrappedQuery.getFullId());
            model.add(subject, RDF.type, Vocab.queryClass);
            model.add(subject, Vocab.queryIDProp, ResourceFactory.createTypedLiteral(wrappedQuery.getId()));
            model.add(subject, RDFS.label, wrappedQuery.getQuery().toString());
        }
        return model;
    }

    @Override
    public Long getResultSize(CloseableHttpResponse response) throws ParserConfigurationException, SAXException, ParseException, IOException {
        Model m;
        try {
            Header contentTypeHeader = response.getEntity().getContentType();
            InputStream inputStream = response.getEntity().getContent();
            m = getModel(contentTypeHeader, inputStream);
        } catch (IllegalAccessException e) {
            LOGGER.error("Could not read response as model", e);
            return -1L;
        }
        return countSize(m);
    }

    @Override
    public Long getResultSize(Header contentTypeHeader, ByteArrayOutputStream content, long contentLength) throws IOException {
        Model m;
        try {
            //TODO BBAIS
            InputStream inputStream = new ByteArrayInputStream(content.toByteArray());
            m = getModel(contentTypeHeader, inputStream);
        } catch (IllegalAccessException e) {
            LOGGER.error("Could not read response as model", e);
            return -1L;
        }
        return countSize(m);
    }

    protected Long countSize(Model m) {
        return m.size();
    }

    private Model getModel(Header contentTypeHeader, InputStream contentInputStream) throws IOException, IllegalAccessException {
        Model m = ModelFactory.createDefaultModel();
        Lang lang = null;
        // get actual content type
        String contentType = contentTypeHeader.getValue();
        // use reflect to iterate over all static Lang fields of the Lang.class
        for (Field langField : Lang.class.getFields()) {
            //create the Language of the field
            Lang susLang = (Lang) langField.get(Lang.class);
            //if they are the same we have our language
            if (contentType.equals(susLang.getContentType().getContentTypeStr())) {
                lang = susLang;
                break;
            }
        }
        if (lang != null)
            m.read(contentInputStream, null, lang.getName());
        return m;
    }
}
