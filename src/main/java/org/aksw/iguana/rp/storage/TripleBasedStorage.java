/**
 * 
 */
package org.aksw.iguana.rp.storage;

import org.aksw.iguana.commons.constants.COMMON;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.Set;

/**
 * This Storage will save all the metric results as triples
 * 
 * @author f.conrads
 *
 */
public abstract class TripleBasedStorage implements Storage {

	protected String baseUri = COMMON.BASE_URI;
	private String resource = COMMON.RES_BASE_URI;
	private String properties = COMMON.PROP_BASE_URI;


	protected Model metricResults = createPrefixModel();

	private String suiteClassUri = baseUri + "/class/Suite";
	private String expClassUri = baseUri + "/class/Experiment";
	private String taskClassUri = baseUri + "/class/Task";
	private String conClassUri = baseUri + "/class/Connection";
	private String datasetClassUri = baseUri + "/class/Dataset";


	private String classUri = RDF.type.getURI();
	private String rdfsUri = "http://www.w3.org/2000/01/rdf-schema#";
	private String xsdUri = "http://www.w3.org/2001/XMLSchema#";


	protected Model createPrefixModel() {
		Model metricResults = ModelFactory.createDefaultModel();
		metricResults.setNsPrefix("iprop", COMMON.PROP_BASE_URI);
		metricResults.setNsPrefix("iont", COMMON.CLASS_BASE_URI);
		metricResults.setNsPrefix("ires", COMMON.RES_BASE_URI);
		metricResults.setNsPrefix("lsqr", "http://lsq.aksw.org/res/");
		return metricResults;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aksw.iguana.rp.storage.Storage#addMetaData(java.util.Properties)
	 */
	@Override
	public void addMetaData(Properties p) {

		String suiteUrl = getUrlWithResourcePrefix(p, COMMON.SUITE_ID_KEY);
		String expUrl = getUrlWithResourcePrefix(p, COMMON.EXPERIMENT_ID_KEY);
		String taskUrl = getUrlWithResourcePrefix(p, COMMON.EXPERIMENT_TASK_ID_KEY);

		String datasetUrl = getUrlWithResourcePrefix(p, COMMON.DATASET_ID_KEY);
		String conName = p.getProperty(COMMON.CONNECTION_ID_KEY);
		if(p.containsKey(COMMON.CONNECTION_VERSION_KEY)){
			conName+="-"+p.getProperty(COMMON.CONNECTION_VERSION_KEY);
		}
		String connUrl = getUrlWithResourcePrefix(conName);

		String actualTaskID = getUrlWithResourcePrefix(p, COMMON.EXPERIMENT_TASK_CLASS_ID_KEY);


		metricResults.add(createStatement(suiteUrl, getUrlWithPropertyPrefix("experiment"), expUrl, true));
		metricResults.add(createStatement(suiteUrl, classUri, suiteClassUri, true));
		metricResults.add(createStatement(expUrl, getUrlWithPropertyPrefix("task"), taskUrl, true));
		metricResults.add(createStatement(expUrl, getUrlWithPropertyPrefix("dataset"), datasetUrl, true));
		metricResults.add(createStatement(expUrl, classUri, expClassUri, true));
		metricResults.add(createStatement(taskUrl, getUrlWithPropertyPrefix("connection"), connUrl, true));
		if(p.containsKey(COMMON.EXPERIMENT_TASK_NAME_KEY)){
			metricResults.add(metricResults.createResource(taskUrl), RDFS.label, p.getProperty(COMMON.EXPERIMENT_TASK_NAME_KEY));
		}

		metricResults.add(createStatement(connUrl, classUri, conClassUri, true));
		metricResults.add(createStatement(datasetUrl, classUri, datasetClassUri, true));
		metricResults.add(createStatement(taskUrl, classUri, taskClassUri, true));
		metricResults.add(createStatement(taskUrl, classUri, actualTaskID, true));

		addExtraMetadata(p, taskUrl);
		metricResults.add(metricResults.createResource(datasetUrl), RDFS.label, p.getProperty(COMMON.DATASET_ID_KEY));
		metricResults.add(metricResults.createResource(connUrl), RDFS.label, p.getProperty(COMMON.CONNECTION_ID_KEY));
		if(p.containsKey(COMMON.CONNECTION_VERSION_KEY)) {
			metricResults.add(metricResults.createResource(connUrl), ResourceFactory.createProperty(getUrlWithPropertyPrefix("version")), p.getProperty(COMMON.CONNECTION_VERSION_KEY));
		}

		if(p.containsKey(COMMON.QUERY_STATS)) {
			Model queryStats = (Model) p.get(COMMON.QUERY_STATS);
			metricResults.add(queryStats);
		}

		Calendar cal = GregorianCalendar.getInstance();
		metricResults.add(metricResults.createResource(taskUrl),
				ResourceFactory.createProperty(rdfsUri + "startDate"), metricResults.createTypedLiteral(cal));
	}

	private String getUrlWithResourcePrefix(Properties p, String key) {
		return getUrlWithResourcePrefix(p.getProperty(key));
	}

	private String getUrlWithResourcePrefix(String suffix) {
		try {
			String[] suffixParts = suffix.split("/");
			for (int i = 0; i < suffixParts.length; i++)
				suffixParts[i] = URLEncoder.encode(suffixParts[i], StandardCharsets.UTF_8.toString());
			return resource + String.join("/", suffixParts);
		} catch (UnsupportedEncodingException e) {
			return resource + suffix.hashCode();
		}
	}

	private String getUrlWithPropertyPrefix(String suffix) {
		try {
			String[] suffixParts = suffix.split("/");
			for (int i = 0; i < suffixParts.length; i++)
				suffixParts[i] = URLEncoder.encode(suffixParts[i], StandardCharsets.UTF_8.toString());
			return properties + String.join("/", suffixParts);
		} catch (UnsupportedEncodingException e) {
			return properties + suffix.hashCode();
		}
	}

	private Statement createStatement(String subject, String predicate, Object object)
	{
			return metricResults.createStatement(metricResults.createResource(subject), ResourceFactory.createProperty(predicate), metricResults.createTypedLiteral(object));
	}

	private Statement createStatement(String subject, String predicate, String object, boolean isObjectUri)
	{
		if(isObjectUri)
			return metricResults.createStatement(metricResults.createResource(subject), ResourceFactory.createProperty(predicate), metricResults.createResource(object));
		else
			return metricResults.createStatement(metricResults.createResource(subject), ResourceFactory.createProperty(predicate), object);
	}

	private void addExtraMetadata(Properties p, String taskUrl) {
		Properties extra = (Properties) p.get(COMMON.EXTRA_META_KEY);
		for (Object obj : extra.keySet()) {
			if (p.containsKey(COMMON.EXTRA_IS_RESOURCE_KEY) && ((Set<?>) p.get(COMMON.EXTRA_IS_RESOURCE_KEY)).contains(obj)) {
				metricResults.add(createStatement(
						taskUrl,
						getUrlWithResourcePrefix(obj.toString()),
						getUrlWithResourcePrefix(extra.get(obj).toString()),
						true));
			} else {
				metricResults.add(createStatement(
						taskUrl,
						getUrlWithPropertyPrefix(obj.toString()),
						extra.get(obj)));
			}
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
		String taskUrl = getUrlWithResourcePrefix(taskID);
		metricResults.add(metricResults.add(metricResults.createResource(taskUrl),
				ResourceFactory.createProperty(rdfsUri + "endDate"), metricResults.createTypedLiteral(cal)));
	}


	public void addData(Model data){
		metricResults.add(data);
	}


}
