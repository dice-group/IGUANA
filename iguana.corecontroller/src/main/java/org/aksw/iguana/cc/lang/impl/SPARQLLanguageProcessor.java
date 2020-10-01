package org.aksw.iguana.cc.lang.impl;

import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.lang.QueryWrapper;
import org.aksw.iguana.cc.utils.SPARQLQueryStatistics;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.vocab.Vocab;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.jena.ext.com.google.common.hash.HashCode;
import org.apache.jena.ext.com.google.common.hash.Hashing;
import org.apache.jena.ext.com.google.common.io.BaseEncoding;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.jcodings.util.Hash;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * SPARQL Language Processor.
 * Tries to analyze Queries as SPARQL queries and checks http response for either application/sparql-results+json
 * or application/sparql-results+xml to count the result size correctly. Otherwise assumes it record per line and counts the returning lines.
 */
@Shorthand("lang.SPARQL")
public class SPARQLLanguageProcessor implements LanguageProcessor {

    private static Logger LOGGER = LoggerFactory.getLogger(SPARQLLanguageProcessor.class);

    public static final String XML_RESULT_ELEMENT_NAME = "result";
    public static final String XML_RESULT_ROOT_ELEMENT_NAME = "results";
    public static final String QUERY_RESULT_TYPE_JSON = "application/sparql-results+json";
    public static final String QUERY_RESULT_TYPE_XML = "application/sparql-results+xml";
    private static final String LSQ_RES = "http://lsq.aksw.org/res/q-";

    @Override
    public String getQueryPrefix() {
        return "sparql";
    }

    @Override
    public Model generateTripleStats(List<QueryWrapper> queries, String resourcePrefix, String taskID) {
        Model model = ModelFactory.createDefaultModel();
        for(QueryWrapper wrappedQuery : queries) {
            Resource subject = ResourceFactory.createResource(COMMON.RES_BASE_URI + resourcePrefix + "/" + wrappedQuery.getId());
            model.add(subject, RDF.type, Vocab.queryClass);
            model.add(subject, Vocab.rdfsID, wrappedQuery.getId().replace("sparql", ""));
            model.add(subject, RDFS.label, wrappedQuery.getQuery().toString());
            try {
                Query q = QueryFactory.create(wrappedQuery.getQuery().toString());
                SPARQLQueryStatistics qs2 = new SPARQLQueryStatistics();
                qs2.getStatistics(q);

                model.add(subject, Vocab.aggrProperty, model.createTypedLiteral(qs2.aggr==1));
                model.add(subject, Vocab.filterProperty, model.createTypedLiteral(qs2.filter==1));
                model.add(subject, Vocab.groupByProperty, model.createTypedLiteral(qs2.groupBy==1));
                model.add(subject, Vocab.havingProperty, model.createTypedLiteral(qs2.having==1));
                model.add(subject, Vocab.triplesProperty, model.createTypedLiteral(qs2.triples));
                model.add(subject, Vocab.offsetProperty, model.createTypedLiteral(qs2.offset==1));
                model.add(subject, Vocab.optionalProperty, model.createTypedLiteral(qs2.optional==1));
                model.add(subject, Vocab.orderByProperty, model.createTypedLiteral(qs2.orderBy==1));
                model.add(subject, Vocab.unionProperty, model.createTypedLiteral(qs2.union==1));
                model.add(subject, OWL.sameAs, getLSQHash(q));
            }catch(Exception e){
                LOGGER.warn("Query statistics could not be created. Not using SPARQL?");
            }
        }
        return model;
    }

    private Resource getLSQHash(Query query){
        String result = MD5(query.toString()).substring(0, 8);;
        return ResourceFactory.createResource(LSQ_RES+result);
    }

    private String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
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
        String contentType = getContentTypeVal(response.getEntity().getContentType());

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
