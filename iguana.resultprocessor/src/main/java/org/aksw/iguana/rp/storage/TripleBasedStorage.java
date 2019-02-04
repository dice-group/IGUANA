/**
 * 
 */
package org.aksw.iguana.rp.storage;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.data.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * This Storage will save all the metric results as triples
 * 
 * @author f.conrads
 *
 */
public abstract class TripleBasedStorage implements Storage {

	protected String baseUri = "http://iguana-benchmark.eu";
	private String resource = baseUri + "/recource/";
	private String properties = baseUri + "/properties/";

	protected StringBuilder blockUpdate = new StringBuilder();
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
		addBlockUpdate(datasetID, p.getProperty(COMMON.DATASET_ID_KEY),  rdfsUri+"label");
		if(p.containsKey(COMMON.SIMPLE_TRIPLE_KEY)) {
			blockUpdate.append(p.get(COMMON.SIMPLE_TRIPLE_KEY));
		}
		
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		addBlockUpdate(taskID, timestamp+"^^<"+xsdUri+"dateTime>",rdfsUri+"startDate");
		
		// Commit Meta Data and clear updateBlock
		commit();
		blockSize = 0;
		blockUpdate = new StringBuilder();
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
		addBlockUpdate(builder.toString(), timestamp+"^^<"+xsdUri+"dateTime>",rdfsUri+"endDate");
	}
	
}
