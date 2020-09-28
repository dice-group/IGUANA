package org.aksw.iguana.cc.lang.impl;

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
public class RDFLanguageProcessor implements LanguageProcessor {

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
            Resource subject = ResourceFactory.createResource(COMMON.RES_BASE_URI + resourcePrefix + "/" + wrappedQuery.getId());
            model.add(subject, RDF.type, Vocab.queryClass);
            model.add(subject, Vocab.rdfsID, wrappedQuery.getId().replace(queryPrefix, "").replace("sparql", ""));
            model.add(subject, RDFS.label, wrappedQuery.getQuery().toString());
        }
        return model;
    }

    @Override
    public Long getResultSize(CloseableHttpResponse response) throws ParserConfigurationException, SAXException, ParseException, IOException {
        Model m = null;
        try {
            m = getModel(response);
        } catch (IllegalAccessException e) {
            LOGGER.error("Could not read response as model", e);
            return -1l;
        }
        Long ret = countSize(m);
        return ret;
    }

    protected Long countSize(Model m) {
        return m.size();
    }

    private Model getModel(CloseableHttpResponse response) throws IOException, IllegalAccessException {
        InputStream inStream = response.getEntity().getContent();
        Model m = ModelFactory.createDefaultModel();
        Lang lang = null;
        // get actual content type
        Header cTypeHeader =response.getEntity().getContentType();
        String cType = cTypeHeader.getValue();
        // use reflect to iterate over all static Lang fields of the Lang.class
        for(Field langField : Lang.class.getFields()){
            //create the Language of the field
            Lang susLang = (Lang)langField.get(Lang.class);
            //if they are the same we have our language
            if(cType.equals(susLang.getContentType().getContentTypeStr())){
                lang = susLang;
                break;
            }
        }
        m.read(inStream, null, lang.getName());
        return m;
    }
}
