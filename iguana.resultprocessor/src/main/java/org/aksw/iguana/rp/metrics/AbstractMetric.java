package org.aksw.iguana.rp.metrics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.config.CONSTANTS;
import org.aksw.iguana.rp.data.Triple;
import org.aksw.iguana.rp.storage.StorageManager;

/**
 * Abstract Metric class which implements the method sendData 
 * so the final Metric class can send their final data via this command to the storages
 * 
 * @author f.conrads
 *
 */
public abstract class AbstractMetric implements Metric{

	protected StorageManager storageManager;
	
	protected Properties metaData;
	
	protected Map<Properties, Properties> dataContainer = new HashMap<Properties, Properties>();
	
	protected String name;
	protected String shortName;
	protected String description;
	
	/**
	 * This constructor will not set name, Short name and description
	 * Thus the final Metric class has to set them itself. 
	 */
	public AbstractMetric(){
	}
	
	/**
	 * Will create an Metric class with the name, short name and description
	 * 
	 * @param name
	 * @param shortName
	 * @param description
	 */
	public AbstractMetric(String name, String shortName, String description){
		this.name=name;
		this.shortName=shortName;
		this.description=description;
	}
	
	@Override
	public void setStorageManager(StorageManager smanager){
		this.storageManager = smanager;
	}
	
	@Override
	public StorageManager getStorageManager(){
		return this.storageManager;
	}
	
	@Override
	public String getDescription(){
		return this.description;
	}
	
	@Override
	public String getName(){
		return this.name;
	}
	
	@Override
	public String getShortName(){
		return this.shortName;
	}
	
	/**
	 * Will send the properties to the storageManager (thus to all the defined storages)
	 * 
	 * @param p
	 */
	protected void sendData(Properties p, Triple[] triples){
		this.storageManager.addData(p, triples);
	}
	
	/**
	 * Will add the Meta Data to the Metric
	 */
	@Override
	public void setMetaData(Properties metaData){
		this.metaData = metaData;
	}
	
	/**
	 * Get a light Meta Properties. </br>
	 * 
	 * setting METRICS_KEY: getShortName()</br>
	 * setting EXPERIMENT_TASK_ID_KEY : taskID</br>
	 * 
	 * @return
	 */
	protected Properties getLightweightMeta(){
		//Create a lightweight meta properties for the correct association
		Properties meta = new Properties();
		meta.setProperty(COMMON.METRICS_PROPERTIES_KEY, getShortName());
		meta.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, metaData.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY));
		return meta;
	}
	
	/**
	 * Will return the Properties Object with the associated key: EXTRA_META_KEY</br>
	 * if this key does not exists: recv will be returned
	 * 
	 * @param recv
	 * @return
	 */
	protected Properties getExtraMeta(Properties recv){
		if(recv.containsKey(COMMON.EXTRA_META_KEY))
			return (Properties) recv.get(COMMON.EXTRA_META_KEY);
		return recv;
	}
	
	/**
	 * Will create {@link org.aksw.iguana.rp.data.Triple} Objects 
	 * with the following structure </br>
	 * subjectNode recv.key_1 recv.value_1</br>
	 * subjectNode recv.key_2 recv.value_2</br>
	 * ...
	 *  
	 * @param subjectNode
	 * @param recv
	 * @return
	 */
	protected Triple[] getExtraMeta(String subjectNode, Properties recv){
		Properties meta = getExtraMeta(recv);
		Triple[] triples = new Triple[meta.size()];
		int i=0;
		for(Object key : meta.keySet()){
			Triple t = new Triple();
			t.setSubject(subjectNode);
			t.setPredicate(key.toString());
			t.setObject(meta.get(key));
			triples[i]=t;
			i++;
		}
		return triples;
	}
	
	/**
	 * 
	 * Will call the storageManager sendData method with the Lightweight Meta and the following created triples</br>
	 * The subject node will be calculated by an empty {@link java.util.Properties} Object and the {@link #getSubjectFromExtraMeta}</br></br>
	 * For each key:value pair: subject results.key results.value</br></br>
	 *
	 * @param results
	 */
	protected void sendTriples(Properties results){
		//set Subject Node, hash out of task ID and if not empty the extra properties
		String subject = getSubjectFromExtraMeta(new Properties());
		sendTriples(subject, results, new HashSet<String>(), new Properties());
	}
	
	/**
	 * 
	 * Will call the storageManager sendData method with the Lightweight Meta and the following created triples</br>
	 * The subject node will be calculated by the recv Object and the {@link #getSubjectFromExtraMeta}</br></br>
	 * For each key:value pair: subject results.key results.value</br></br>
	 * The getExtraMeta() method will be called, with the recv Property and the subject node
	 * Thus the following triples will be added:</br>
	 * For each key:value pair: subject recv.key recv.value 
	 * 
	 * @param results
	 * @param recv
	 */
	protected void sendTriples(Properties results, Properties recv){
		//set Subject Node, hash out of task ID and if not empty the extra properties
		String subject = getSubjectFromExtraMeta(recv);
		sendTriples(subject, results, new HashSet<String>(), recv);
	}

	/**
	 * 
	 * Will call the storageManager sendData method with the Lightweight Meta and the following created triples</br>
	 * The triples add will be simply added</br>
	 * The subject node will be calculated from the recv Object wiht the {@link #getSubjectFromExtraMeta} Method</br></br>
	 * For each key:value pair: subject results.key results.value</br>
	 * Be aware that the Set isResource will set if the key/value will be handled as resources.
	 * If the key (value) is in the set it will be handled as a resource</br></br>
	 * The getExtraMeta() method will be called, with the recv Property and the subject node
	 * Thus the following triples will be added:</br>
	 * For each key:value pair: subject recv.key recv.value 
	 * 
	 * @param results
	 * @param isResource
	 * @param recv
	 * @param add
	 */
	protected void sendTriples(Properties results, Set<String> isResource, Properties recv, Triple[] add){
		//set Subject Node, hash out of task ID and if not empty the extra properties
		String subject = getSubjectFromExtraMeta(recv);
		sendTriples(subject, results, isResource, recv, add);
	}
	
	/**
	 *
	 * Will call the storageManager sendData method with the Lightweight Meta and the following created triples</br>
	 * The subject node will be the subject for every following triple</br></br>
	 * For each key:value pair: subject results.key results.value</br>
	 * Be aware that the Set isResource will set if the key/value will be handled as resources.
	 * If the key (value) is in the set it will be handled as a resource</br></br>
	 * The getExtraMeta() method will be called, with the recv Property and the subject node
	 * Thus the following triples will be added:</br>
	 * For each key:value pair: subject recv.key recv.value 
	 *
	 * @param subject
	 * @param results
	 * @param isResource
	 * @param recv
	 */
	protected void sendTriples(String subject, Properties results, Set<String> isResource, Properties recv){
		sendTriples(subject, results, isResource, recv, new Triple[0]);
	}
	
	/**
	 * 
	 * Will call the storageManager sendData method with the Lightweight Meta and the following created triples</br>
	 * The triples add will be simply added</br>
	 * The subject node will be the subject for every following triple</br></br>
	 * For each key:value pair: subject results.key results.value</br>
	 * Be aware that the Set isResource will set if the key/value will be handled as resources.
	 * If the key (value) is in the set it will be handled as a resource</br></br>
	 * The getExtraMeta() method will be called, with the recv Property and the subject node
	 * Thus the following triples will be added:</br>
	 * For each key:value pair: subject recv.key recv.value 
	 * 
	 * @param subject
	 * @param results
	 * @param isResource
	 * @param recv
	 * @param add
	 */
	protected void sendTriples(String subject, Properties results, Set<String> isResource, Properties recv, Triple[] add){
		
		//get Extra Meta 
		Triple[] extraTriples = getExtraMeta(subject, recv);
		Properties lw = getLightweightMeta();
		lw.put(CONSTANTS.LENGTH_EXTRA_META_KEY, extraTriples.length);
		Triple[] triples = new Triple[extraTriples.length+results.size()+add.length];
		int i=0;
		for(;i<extraTriples.length;i++){
			triples[i] = extraTriples[i];
		}
		
		//Add Result
		for(Object obj : results.keySet()){
			Triple resultT = new Triple();
			resultT.setSubject(subject);
			resultT.setPredicate(obj.toString());
			resultT.setObject(results.get(obj));
			if(isResource.contains(obj.toString())){
				resultT.setPredicateResource(true);
			}
			if(isResource.contains(results.get(obj).toString())){
				resultT.setObjectResource(true);
			}
			triples[i] = resultT;
			i++;
		}
		for(int j=0;i<triples.length;i++){
			triples[i] = add[j];
			j++;
		}
		
		sendData(lw, triples);
	}
	
	/**
	 * Will create a subject node string from the recv object (ExperimentTaskID and extraMeta Hash)
	 * @param recv
	 * @return
	 */
	protected String getSubjectFromExtraMeta(Properties recv){
		String subject = metaData.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY);
		Properties extraMeta = getExtraMeta(recv);
		if (!extraMeta.isEmpty()) {
			subject += "/" + extraMeta.hashCode();
		}
		return subject;
	}
	
	/**
	 * Will add the data to a in  memory container which can be assessed by extra
	 * 
	 * @param extra
	 * @param data
	 */
	protected void addDataToContainer(Properties extra, Properties data){
		this.dataContainer.put(extra, data);
	}

	/**
	 * Getting the data Properties from the data container associated to extra
	 * 
	 * @param extra
	 * @return
	 */
	protected Properties getDataFromContainer(Properties extra){
		return this.dataContainer.get(extra);
	}
	
	/**
	 * Assuming that the results are Integer objects, this will 
	 * 1. if no data for extra exists, create the data from the results object
	 * 2. if the data exists, sum the corresponding 
	 * 
	 * for example:  
	 * container has data object e1:(a:10, b:12)
	 * new results for e1 are (a:2, b:5)
	 * The new container data will be (a:12, b:17) 
	 * 
	 * @param extra
	 * @param results
	 */
	protected void processData(Properties extra, Properties results){
		Properties tmp = getDataFromContainer(extra);
		if(tmp!=null){
			for(Object obj : results.keySet()){
				Integer res = (Integer) tmp.get(obj.toString());
				tmp.put(obj.toString(),res+(Integer)results.get(obj));
			}
		}
		else{
			tmp = new Properties();
			for(Object obj : results.keySet()){
				tmp.put(obj.toString(),(Integer)results.get(obj));
			}
		}
		addDataToContainer(extra, tmp);
	}
}
