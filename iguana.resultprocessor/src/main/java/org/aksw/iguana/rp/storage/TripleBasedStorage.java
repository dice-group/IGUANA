/**
 * 
 */
package org.aksw.iguana.rp.storage;

import java.io.*;
import java.sql.Timestamp;
import java.util.*;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.data.Triple;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.datatypes.xsd.impl.XSDDateTimeType;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * This Storage will save all the metric results as triples
 * 
 * @author f.conrads
 *
 */
public abstract class TripleBasedStorage implements Storage {

	protected String baseUri = "http://iguana-benchmark.eu";
	private String resource = baseUri + "/resource/";
	private String properties = baseUri + "/properties/";

	protected StringBuilder blockUpdate = new StringBuilder();
	protected Model metricResults = ModelFactory.createDefaultModel();
	private int blockSize = 0;

	protected int maxBlockSize = 50;
	private String suiteClassUri = baseUri + "/class/Suite";
	private String expClassUri = baseUri + "/class/Experiment";
	private String taskClassUri = baseUri + "/class/Task";

	private String classUri = "http://www.w3.org/2000/01/rdf-schema#Class";
	private String rdfsUri = "http://www.w3.org/2000/01/rdf-schema#";
	private String xsdUri = "http://www.w3.org/2001/XMLSchema#";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aksw.iguana.rp.storage.Storage#addData(java.util.Properties)
	 */
	@Override
	public void addData(Properties meta, Triple[] triples) {
		//Graph based approach start
		for(Triple t : triples)
		{
			Resource subject = metricResults.createResource(this.resource + t.getSubject());
			Property predicate = ResourceFactory.createProperty(properties + t.getPredicate());
			RDFNode object;
			if (t.isObjectResource()) {
				object = metricResults.createResource(this.resource + t.getObject());
			} else {
				object = metricResults.createTypedLiteral(t.getObject());
			}
			metricResults.add(subject, predicate, object);
		}
		//Graph based approach end

		// Following is commented to implement graph based approach instead of StringBuilder based approach
		// Add Data to Block, as soon as Block is big enough, commit
		StringBuilder builder = new StringBuilder();
		// Add Node to connect to
		builder.append("<").append(resource).append(meta.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY)).append(">");
		// Add metrics name as property
		builder.append(" ").append("<").append(resource).append(meta.getProperty(COMMON.METRICS_PROPERTIES_KEY))
				.append(">").append(" ");

		Set<String> deduplicate = getDeduplication(builder, triples);
		// deduplicate to blockUpdate
		for (String triple : deduplicate) {
			blockSize++;
			blockUpdate.append(triple).append(" . \n");
		}

		if (maxBlockSize <= blockSize) {
			// Commit and clear as everything is updated
			commit();
			blockSize = 0;
			blockUpdate = new StringBuilder();
		}
	}

	private Set<String> getDeduplication(StringBuilder builder, Triple[] triples) {
		// Deduplicate with HashSet
		Set<String> deduplicate = new HashSet<String>();

		for (Triple triple : triples) {

			StringBuilder builder2 = new StringBuilder();
			builder2.append(" <").append(resource).append(triple.getSubject()).append("> ");
			deduplicate.add(builder.toString() + builder2.toString());

			builder2.append(" <").append(properties).append(triple.getPredicate()).append("> ");

			if (triple.isObjectResource()) {
				builder2.append(" <").append(resource).append(triple.getObject()).append("> ");
			} else {
				Model empty = ModelFactory.createDefaultModel();
				Literal l = empty.createTypedLiteral(triple.getObject());
				String obj = "\"" + l.getValue() + "\"^^<" + l.getDatatypeURI() + ">";
				builder2.append(obj);
			}

			deduplicate.add(builder2.toString());
		}
		return deduplicate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aksw.iguana.rp.storage.Storage#addMetaData(java.util.Properties)
	 */
	@Override
	public void addMetaData(Properties p) {
		// Add MetaData and commit.
		// Make sure Updates are empty
		commit();

		//TODO REMOVE LATER, dummy to prevent outputting metadata
		if(!p.containsKey("sdhfhjshdfjkhskdfk"))
			return;

		// Suite ID
		String suiteID = getID(p, COMMON.SUITE_ID_KEY);
		// Experiment ID
		String expID = getID(p, COMMON.EXPERIMENT_ID_KEY);
		// Experiment Task ID
		String taskID = getID(p, COMMON.EXPERIMENT_TASK_ID_KEY);
		// Dataset ID
		String datasetID = getID(p, COMMON.DATASET_ID_KEY);
		// Connection ID
		String connID = getID(p, COMMON.CONNECTION_ID_KEY);

		// Actual triples to commit
		addBlockUpdate(suiteID, expID, properties+"experiment");
		addBlockUpdate(suiteID, "<" + suiteClassUri + ">", classUri);
		addBlockUpdate(expID, taskID, properties+"task");
		addBlockUpdate(expID, datasetID, properties+"dataset");
		addBlockUpdate(expID, "<" + expClassUri + ">", classUri);
		addBlockUpdate(taskID, connID, properties+"connection");
		addBlockUpdate(taskID, "<" + taskClassUri + ">", classUri);
		addBlockUpdateExtra(p, taskID);
		addBlockUpdate(datasetID, "\""+p.getProperty(COMMON.DATASET_ID_KEY)+"\"",  rdfsUri+"label");
		addBlockUpdate(connID, "\""+p.getProperty(COMMON.CONNECTION_ID_KEY)+"\"",  rdfsUri+"label");
		if(p.containsKey(COMMON.SIMPLE_TRIPLE_KEY)) {
			blockUpdate.append(p.get(COMMON.SIMPLE_TRIPLE_KEY));
		}

		Calendar cal = GregorianCalendar.getInstance();
		Timestamp timestamp = new Timestamp(cal.getTimeInMillis());
		addBlockUpdate(taskID, "\""+timestamp+"\"^^<"+xsdUri+"dateTime>",rdfsUri+"startDate");

		// Graph approach start
		String suiteUrl = getUrlWithResourcePrefix(p, COMMON.SUITE_ID_KEY);
		String expUrl = getUrlWithResourcePrefix(p, COMMON.EXPERIMENT_ID_KEY);
		String taskUrl = getUrlWithResourcePrefix(p, COMMON.EXPERIMENT_TASK_ID_KEY);
		String datasetUrl = getUrlWithResourcePrefix(p, COMMON.DATASET_ID_KEY);
		String connUrl = getUrlWithResourcePrefix(p, COMMON.CONNECTION_ID_KEY);

		metricResults.add(createStatement(suiteUrl, getUrlWithPropertyPrefix("experiment"), expUrl, true));
		metricResults.add(createStatement(suiteUrl, classUri, suiteClassUri, true));
		metricResults.add(createStatement(expUrl, getUrlWithPropertyPrefix("task"), taskUrl, true));
		metricResults.add(createStatement(expUrl, getUrlWithPropertyPrefix("dataset"), datasetUrl, true));
		metricResults.add(createStatement(expUrl, classUri, expClassUri, true));
		metricResults.add(createStatement(taskUrl, getUrlWithPropertyPrefix("connection"), connUrl, true));
		metricResults.add(createStatement(taskUrl, classUri, taskClassUri, true));
		addExtraMetadata(p, taskUrl);
		metricResults.add(metricResults.createResource(datasetUrl), RDFS.label, p.getProperty(COMMON.DATASET_ID_KEY));
		metricResults.add(metricResults.createResource(connUrl), RDFS.label, p.getProperty(COMMON.CONNECTION_ID_KEY));

		if(p.containsKey(COMMON.QUERY_STATS)) {
			Model queryStats = (Model) p.get(COMMON.QUERY_STATS);
			metricResults.add(queryStats);
		}

		metricResults.add(metricResults.add(metricResults.createResource(taskUrl),
				ResourceFactory.createProperty(rdfsUri + "startDate"), metricResults.createTypedLiteral(cal)));
		// Graph approach end

		// Commit Meta Data and clear updateBlock
		commit();
		blockSize = 0;
		blockUpdate = new StringBuilder();
	}

	private String getUrlWithResourcePrefix(Properties p, String key) {
		return getUrlWithResourcePrefix(p.getProperty(key));
	}

	private String getUrlWithResourcePrefix(String suffix) {
		return resource + suffix;
	}

	private String getUrlWithPropertyPrefix(String suffix) {
		return properties + suffix;
	}

	private Statement createStatement(String subject, String predicate, String object, boolean isObjectUri)
	{
		if(isObjectUri)
			return metricResults.createStatement(metricResults.createResource(subject), ResourceFactory.createProperty(predicate), metricResults.createResource(object));
		else
			return metricResults.createStatement(metricResults.createResource(subject), ResourceFactory.createProperty(predicate), object);
	}

	private void addBlockUpdateExtra(Properties p, String taskID) {
		Properties extra = (Properties) p.get(COMMON.EXTRA_META_KEY);
		for (Object obj : extra.keySet()) {
			blockUpdate.append(taskID);
			if (p.containsKey(COMMON.EXTRA_IS_RESOURCE_KEY)
					&& ((Set<?>) p.get(COMMON.EXTRA_IS_RESOURCE_KEY)).contains(obj)) {
				blockUpdate.append(" <").append(resource).append(obj.toString()).append("> ");
			} else {
				blockUpdate.append(" <").append(properties).append(obj.toString()).append("> ");
			}
			if (p.containsKey(COMMON.EXTRA_IS_RESOURCE_KEY)
					&& ((Set<?>) p.get(COMMON.EXTRA_IS_RESOURCE_KEY)).contains(obj)) {
				blockUpdate.append(" <").append(resource).append(extra.get(obj)).append("> .\n");
			} else {
				blockUpdate.append("\"").append(extra.get(obj)).append("\"").append(" . \n");
			}
		}
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
						extra.get(obj).toString(),
						false));
			}
		}
	}


	private String getID(Properties p, String key) {
		StringBuilder builder = new StringBuilder();
		builder.append("<").append(resource).append(p.getProperty(key)).append(">");
		return builder.toString();
	}

	private void addBlockUpdate(String subjectURI, String objectURI, String predicate) {
		blockUpdate.append(subjectURI).append("  <").append(predicate).append(">  ").append(objectURI)
				.append(" .\n");
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	public void endTask(String taskID) {
		StringBuilder builder = new StringBuilder();
		builder.append("<").append(resource).append(taskID).append(">");

		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		addBlockUpdate(builder.toString(), "\""+timestamp+"\"^^<"+xsdUri+"dateTime>",rdfsUri+"endDate");
	}

	@Override
	public Model getDataModel() {
		return metricResults;
	}

	public static void main(String[] args) {
		try(InputStream os = new FileInputStream("results_task_531624254-1-1.nt_end")) {
			Model mm = ModelFactory.createDefaultModel();
			RDFDataMgr.read(mm, os, Lang.NT);

			List<Statement> allR = mm.listStatements(null, ResourceFactory.createProperty("http://iguana-benchmark.eu/properties/qps#query"), (RDFNode) null).toList();
			System.out.println(allR.size());

		} catch (IOException e) {
			System.out.println("Could not read");
		}

	}
}
