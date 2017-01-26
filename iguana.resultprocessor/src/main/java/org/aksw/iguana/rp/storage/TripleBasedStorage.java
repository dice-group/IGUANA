/**
 * 
 */
package org.aksw.iguana.rp.storage;

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
	private String resource = baseUri+"/recource/";
	private String properties = baseUri+"/properties/";
	
	protected StringBuilder blockUpdate = new StringBuilder();
	private int blockSize=0;
	
	protected int maxBlockSize=50;
	private String suiteClassUri = baseUri+"/class/Suite";
	private String expClassUri = baseUri+"/class/Experiment";
	private String taskClassUri = baseUri+"/class/Task";


	private String classUri = "http://www.w3.org/2000/01/rdf-schema#Class";
	
	
	/* (non-Javadoc)
	 * @see org.aksw.iguana.rp.storage.Storage#addData(java.util.Properties)
	 */
	@Override
	public void addData(Properties meta, Triple[] triples) {
		//Add Data to Block, as soon as Block is big enough, commit
		StringBuilder builder = new StringBuilder();
		//Add Node to connect to
		builder.append("<").append(resource)
			.append(meta.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY)).append(">");
		//Add metrics name as property
		builder.append(" ").append("<").append(resource)
			.append(meta.getProperty(COMMON.METRICS_PROPERTIES_KEY)).append(">").append(" ");
		
		//Deduplicate with HashSet
		Set<String> deduplicate = new HashSet<String>();
		
		for(Triple triple : triples){
			
			StringBuilder builder2 = new StringBuilder();
			builder2.append(" <").append(resource)
				.append(triple.getSubject()).append("> ");
			deduplicate.add(builder.toString()+builder2.toString());
			
			builder2.append(" <").append(properties)
				.append(triple.getPredicate()).append("> ");

			if(triple.isObjectResource()){
				builder2.append(" <").append(resource)
					.append(triple.getObject()).append("> ");
			}
			else{
				Model empty = ModelFactory.createDefaultModel();
				Literal l = empty.createTypedLiteral(triple.getObject());
				String obj="\""+l.getValue()+"\"^^<"+l.getDatatypeURI()+">";
				builder2.append(obj);
			}
			
			deduplicate.add(builder2.toString());
		}
		
		//deduplicate to blockUpdate
		for(String triple : deduplicate){
			blockSize++;
			blockUpdate.append(triple).append(" . \n");
		}
		
		if(maxBlockSize<=blockSize){
			//Commit and clear as everything is updated
			commit();
			blockSize=0;
			blockUpdate = new StringBuilder();
		}
	}

	/* (non-Javadoc)
	 * @see org.aksw.iguana.rp.storage.Storage#addMetaData(java.util.Properties)
	 */
	@Override
	public void addMetaData(Properties p) {
		//Add MetaData and commit.
		//Make sure Updates are empty
		commit();
		
		StringBuilder builder = new StringBuilder();
		
		//Suite ID
		builder.append(" <").append(resource)
			.append(p.getProperty(COMMON.SUITE_ID_KEY)).append("> ");
		String suiteID = builder.toString();

		//Experiment ID
		builder = new StringBuilder();
		builder.append(" <").append(resource)
			.append(p.getProperty(COMMON.EXPERIMENT_ID_KEY)).append("> ");
		String expID = builder.toString();
			
		//Experiment Task ID
		builder = new StringBuilder();
		builder.append(" <").append(resource)
			.append(p.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY)).append("> ");
		String taskID = builder.toString();
		
		//Dataset ID
		builder = new StringBuilder();
		builder.append(" <").append(resource)
			.append(p.getProperty(COMMON.DATASET_ID_KEY)).append("> ");
		String datasetID = builder.toString();
		
		//Connection ID
		builder = new StringBuilder();
		builder.append(" <").append(resource)
			.append(p.getProperty(COMMON.CONNECTION_ID_KEY)).append("> ");
		String connID = builder.toString();
	
		//Actual triples to commit
		blockUpdate.append(suiteID)
			.append(" <").append(properties)
			.append("experiment").append("> ")
			.append(expID).append(".\n");
		blockUpdate.append(suiteID)
			.append(" <").append(classUri).append("> ")
			.append(" <").append(suiteClassUri).append("> .\n");
		
		blockUpdate.append(expID)
			.append(" <").append(properties)
			.append("task").append("> ")
			.append(taskID).append(".\n");
		blockUpdate.append(expID)
			.append(" <").append(properties)
			.append("dataset").append("> ")
			.append(datasetID).append(".\n");
		blockUpdate.append(expID)
			.append(" <").append(classUri).append("> ")
			.append(" <").append(expClassUri).append("> .\n");
		
		Properties extra = (Properties)p.get(COMMON.EXTRA_META_KEY);
		for(Object obj : extra.keySet()){
			blockUpdate.append(expID);
			if(p.containsKey(COMMON.EXTRA_IS_RESOURCE_KEY) &&
					((Set<?>)p.get(COMMON.EXTRA_IS_RESOURCE_KEY)).contains(obj)){
				blockUpdate.append(" <").append(resource).append(obj.toString()).append("> ");
			}
			else{
				blockUpdate.append(" <").append(properties).append(obj.toString()).append("> ");
			}
			if(p.containsKey(COMMON.EXTRA_IS_RESOURCE_KEY) && ((Set<?>)p.get(COMMON.EXTRA_IS_RESOURCE_KEY)).contains(obj)){
				blockUpdate.append(" <").append(resource).append(extra.get(obj)).append("> .\n");
			}
			else{
				blockUpdate.append("\"").append(extra.get(obj)).append("\"").append(" . \n");
			}
		}
		
		
		blockUpdate.append(taskID)
			.append(" <").append(properties)
			.append("connection").append("> ")
			.append(connID).append(".\n");
		blockUpdate.append(taskID)
			.append(" <").append(classUri).append("> ")
			.append(" <").append(taskClassUri).append("> .\n");
	
		//Commit Meta Data and clear updateBlock
		commit();
		blockSize=0;
		blockUpdate = new StringBuilder();
	}


	@Override
	public String toString(){
		return this.getClass().getSimpleName();
	}
	
}
