/**
 *
 */
package org.aksw.iguana.rp.storage;

import java.math.BigDecimal;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.vocab.Vocab;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;

/**
 * This Storage will save all the metric results as triples
 *
 * @author f.conrads
 *
 */
public abstract class TripleBasedStorage implements Storage {

    protected String baseUri = COMMON.BASE_URI;

    protected Model metricResults = ModelFactory.createDefaultModel();


    /*
     * (non-Javadoc)
     *
     * @see org.aksw.iguana.rp.storage.Storage#addMetaData(java.util.Properties)
     */
    @Override
    public void addMetaData(Properties p) {


        Resource suiteUrl = getUrlWithResourcePrefix(p, COMMON.SUITE_ID_KEY);
        Resource expUrl = getUrlWithResourcePrefix(p, COMMON.EXPERIMENT_ID_KEY);
        Resource taskUrl = getUrlWithResourcePrefix(p, COMMON.EXPERIMENT_TASK_ID_KEY);
        Resource datasetUrl = getUrlWithResourcePrefix(p, COMMON.DATASET_ID_KEY);
        Resource connUrl = getUrlWithResourcePrefix(p, COMMON.CONNECTION_ID_KEY);
        Resource actualTaskID = getUrlWithResourcePrefix(p, COMMON.EXPERIMENT_TASK_CLASS_ID_KEY);


        metricResults.add(suiteUrl, Vocab.experiment, expUrl);
        metricResults.add(suiteUrl, RDF.type, Vocab.suiteClass);
        metricResults.add(expUrl, Vocab.task, taskUrl);
        metricResults.add(expUrl, Vocab.dataset, datasetUrl);
        metricResults.add(expUrl, RDF.type, Vocab.experimentClass);
        metricResults.add(taskUrl, Vocab.connection, connUrl);
        metricResults.add(taskUrl, RDF.type, Vocab.taskClass);
        metricResults.add(taskUrl, RDF.type, actualTaskID);
        metricResults.add(connUrl, RDF.type, Vocab.connectionClass);
        metricResults.add(datasetUrl, RDF.type, Vocab.datasetClass);

        addExtraMetadata(p, taskUrl);
        metricResults.add(metricResults.createResource(datasetUrl), RDFS.label, p.getProperty(COMMON.DATASET_ID_KEY));
        metricResults.add(metricResults.createResource(connUrl), RDFS.label, p.getProperty(COMMON.CONNECTION_ID_KEY));

        if (p.containsKey(COMMON.QUERY_STATS)) {
            Model queryStats = (Model) p.get(COMMON.QUERY_STATS);
            metricResults.add(queryStats);
        }

        Calendar cal = GregorianCalendar.getInstance();
        metricResults.add(taskUrl, Vocab.startDateProp, metricResults.createTypedLiteral(cal));
    }

    private Resource getUrlWithResourcePrefix(Properties p, String key) {
        return getUrlWithResourcePrefix(p.getProperty(key));
    }

    private String urlEncodePath(String urlPath) {
        try {
            String[] suffixParts = urlPath.split("/");
            for (int i = 0; i < suffixParts.length; i++)
                suffixParts[i] = URLEncoder.encode(suffixParts[i], StandardCharsets.UTF_8.toString());
            return String.join("/", suffixParts);
        } catch (UnsupportedEncodingException e) {
            return String.valueOf(urlPath.hashCode());
        }

    }

    private Resource getUrlWithResourcePrefix(String suffix) {
        return ResourceFactory.createResource(COMMON.RES_BASE_URI + urlEncodePath(suffix));
    }

    private Property getUrlWithPropertyPrefix(String suffix) {
        return ResourceFactory.createProperty(COMMON.PROP_BASE_URI + urlEncodePath(suffix));
    }


    private void addExtraMetadata(Properties p, Resource task) {
        Properties extra = (Properties) p.get(COMMON.EXTRA_META_KEY);
        for (Object obj : extra.keySet()) {
            Object value = extra.get(obj);

            if (value instanceof Integer || value instanceof Long) {
                long long_value = ((Number) value).longValue();
                metricResults.add(task, getUrlWithPropertyPrefix(obj.toString()), ResourceFactory.createTypedLiteral(BigInteger.valueOf(long_value)));
            }
            if (value instanceof Float || value instanceof Double) {
                double double_value = ((Number) value).doubleValue();
                metricResults.add(task, getUrlWithPropertyPrefix(obj.toString()), ResourceFactory.createTypedLiteral(BigDecimal.valueOf(double_value)));
            } else
                metricResults.add(task, getUrlWithPropertyPrefix(obj.toString()), ResourceFactory.createTypedLiteral(value));
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    /**
     * Ends the task and adds a rdfs:endDate triple with the current time
     * @param taskID
     */
    public void endTask(String taskID) {
        Calendar cal = GregorianCalendar.getInstance();
        Resource task = getUrlWithResourcePrefix(taskID);
        metricResults.add(task, Vocab.endDateProp, metricResults.createTypedLiteral(cal));
    }


    public void addData(Model data) {
        metricResults.add(data);
    }


}
