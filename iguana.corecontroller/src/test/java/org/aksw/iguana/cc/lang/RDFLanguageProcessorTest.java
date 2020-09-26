package org.aksw.iguana.cc.lang;

import org.aksw.iguana.cc.lang.impl.RDFLanguageProcessor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class RDFLanguageProcessorTest {

    private static Logger LOGGER = LoggerFactory.getLogger(RDFLanguageProcessorTest.class);
    private final Lang lang;
    private final Model m;

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IllegalAccessException {
        Collection<Object[]> testData = new ArrayList<Object[]>();
        for(Field langField : Lang.class.getFields()) {
            Lang susLang = (Lang)langField.get(Lang.class);
            if(susLang.equals(Lang.RDFTHRIFT) || susLang.equals(Lang.TRIX) || susLang.equals(Lang.SHACLC) || susLang.equals(Lang.TSV) || susLang.equals(Lang.CSV) || susLang.equals(Lang.RDFNULL)) {
                //cannot test them as model doesn't allow them to write
                continue;
            }
            testData.add(new Object[]{susLang});
        }
        return testData;
    }
    
    public RDFLanguageProcessorTest(Lang lang){
        this.lang = lang;
        this.m = ModelFactory.createDefaultModel();
        m.add(ResourceFactory.createResource("uri://test"), ResourceFactory.createProperty("uri://prop1"), "abc");
        m.add(ResourceFactory.createResource("uri://test"), ResourceFactory.createProperty("uri://prop2"), "abc2");
        LOGGER.info("Testing Lanuage {} Content-Type: {}", lang.getName(), lang.getContentType());
    }
    
    @Test
    public void testCorrectModel() throws IOException, ParserConfigurationException, SAXException, ParseException {
        StringWriter sw = new StringWriter();
        m.write(sw, lang.getName(), null);
        CloseableHttpResponse response = MockCloseableHttpResponse.buildMockResponse(sw.toString(), lang.getContentType().getContentTypeStr());
        RDFLanguageProcessor processor = new RDFLanguageProcessor();
        assertEquals(2, processor.getResultSize(response).longValue());
    }

    
}
