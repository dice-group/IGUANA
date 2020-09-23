package org.aksw.iguana.cc.lang.impl;

import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.lang.QueryWrapper;
import org.aksw.iguana.cc.utils.QueryStatistics;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.vocab.Vocab;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Shorthand("lang.SPARQL")
public class SPARQLLanguageProcessor implements LanguageProcessor {

    private static Logger LOGGER = LoggerFactory.getLogger(SPARQLLanguageProcessor.class);

    public static final String XML_RESULT_ELEMENT_NAME = "result";
    public static final String XML_RESULT_ROOT_ELEMENT_NAME = "results";
    public static final String QUERY_RESULT_TYPE_JSON = "application/sparql-results+json";
    public static final String QUERY_RESULT_TYPE_XML = "application/sparql-results+xml";


    @Override
    public Model generateTripleStats(List<QueryWrapper> queries, String resourcePrefix, String taskID) {
        Model model = ModelFactory.createDefaultModel();
        for(QueryWrapper wrappedQuery : queries) {
            try {
                Query q = QueryFactory.create(wrappedQuery.getQuery().toString());
                QueryStatistics qs2 = new QueryStatistics();
                qs2.getStatistics(q);

                Resource subject = ResourceFactory.createResource(COMMON.RES_BASE_URI + resourcePrefix + "/" + wrappedQuery.getId());
                model.add(subject, RDF.type, Vocab.queryClass);
                model.add(subject, Vocab.rdfsID, wrappedQuery.getId().replace("sparql", ""));
                model.add(subject, RDFS.label, wrappedQuery.getQuery().toString());
                model.add(subject, Vocab.aggrProperty, model.createTypedLiteral(qs2.aggr==1));
                model.add(subject, Vocab.filterProperty, model.createTypedLiteral(qs2.filter==1));
                model.add(subject, Vocab.groupByProperty, model.createTypedLiteral(qs2.groupBy==1));
                model.add(subject, Vocab.havingProperty, model.createTypedLiteral(qs2.having==1));
                model.add(subject, Vocab.triplesProperty, model.createTypedLiteral(qs2.triples));
                model.add(subject, Vocab.offsetProperty, model.createTypedLiteral(qs2.offset==1));
                model.add(subject, Vocab.optionalProperty, model.createTypedLiteral(qs2.optional==1));
                model.add(subject, Vocab.orderByProperty, model.createTypedLiteral(qs2.orderBy==1));
                model.add(subject, Vocab.unionProperty, model.createTypedLiteral(qs2.union==1));
            }catch(Exception e){
                LOGGER.error("Query statistics could not be created. Not using SPARQL? Will not attach them to results.", e);
            }
        }
        return model;
    }

    public static String getContentTypeVal(Header header) {
        for (HeaderElement el : header.getElements()) {
            NameValuePair cTypePair = el.getParameterByName("Content-Type");

            if (cTypePair != null && !cTypePair.getValue().isEmpty()) {
                return cTypePair.getValue();
            }
        }
        int index = header.toString().indexOf("Content-Type");
        if (index >= 0) {
            String ret = header.toString().substring(index + "Content-Type".length() + 1);
            if (ret.contains(";")) {
                return ret.substring(0, ret.indexOf(";")).trim();
            }
            return ret.trim();
        }
        return "application/sparql-results+json";
    }

    public static long getJsonResultSize(String res) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(res.trim());
        long size = ((JSONArray) ((JSONObject) json.get("results")).get("bindings")).size();
        return size;
    }

    public static long getXmlResultSize(String res) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        ByteArrayInputStream input = new ByteArrayInputStream(res.getBytes(StandardCharsets.UTF_8));
        Document doc = dBuilder.parse(input);
        NodeList childNodes = doc.getDocumentElement().getElementsByTagName(XML_RESULT_ROOT_ELEMENT_NAME).item(0).getChildNodes();

        long size = 0;
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (XML_RESULT_ELEMENT_NAME.equalsIgnoreCase(childNodes.item(i).getNodeName())) {
                size++;
            }
        }
        return size;

    }

    @Override
    public Long getResultSize(CloseableHttpResponse response) throws ParserConfigurationException, SAXException, ParseException, IOException {
        HttpEntity httpResponse = response.getEntity();
        String contentType = getContentTypeVal(response.getHeaders("Content-Type")[0]);

        try (InputStream inputStream = httpResponse.getContent();
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                //br.readline will remove \n (add it for no Content-Type mode)
                result.append(line).append("\n");
            }
            long resultSize;
            if (QUERY_RESULT_TYPE_JSON.equals(contentType)) {
                resultSize = getJsonResultSize(result.toString());
            } else if (QUERY_RESULT_TYPE_XML.equals(contentType)) {
                resultSize = getXmlResultSize(result.toString());
            } else {
                resultSize = StringUtils.countMatches(result.toString(), "\n");
            }

            return resultSize;

        } catch (IOException | ParseException | ParserConfigurationException | SAXException e) {
            LOGGER.error("Query results could not be parsed: ", e);
            throw e;
        }
    }
}
