package org.aksw.iguana.rp.metrics;

import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.storage.StorageManager;
import org.aksw.iguana.rp.vocab.Vocab;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Abstract Metric class which implements the method sendData 
 * so the final Metric class can send their final data via this command to the storages
 * 
 * @author f.conrads
 *
 */
public abstract class AbstractMetric implements Metric{

	protected StorageManager storageManager = StorageManager.getInstance();

	protected Properties metaData = new Properties();

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
	 * Will add the Meta Data to the Metric
	 */
	@Override
	public void setMetaData(Properties metaData){
		this.metaData = metaData;
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
	 * Will create a subject node string from the recv object (ExperimentTaskID and extraMeta Hash)
	 * @param recv
	 * @return
	 */
	protected String getSubjectFromExtraMeta(Properties recv){
		String subject = metaData.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY);
		Properties extraMeta = getExtraMeta(recv);
		if (!extraMeta.isEmpty()) {
			subject += "/" + recv.get(COMMON.WORKER_ID);
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
				if(tmp.get(obj.toString()) instanceof Long) {
					Long res = (long) tmp.get(obj.toString());
					tmp.put(obj.toString(),res+(long)results.get(obj));
				}
				else if(tmp.get(obj.toString()) instanceof Integer) {
					int res = (int) tmp.get(obj.toString());
					tmp.put(obj.toString(),res+(int)results.get(obj));
				}
				else if(tmp.get(obj.toString()) instanceof Double) {
					double res = (double) tmp.get(obj.toString());
					tmp.put(obj.toString(),res+(double)results.get(obj));
				}
			}
		}
		else{
			tmp = new Properties();
			for(Object obj : results.keySet()){
				if(results.get(obj) instanceof Long)
					tmp.put(obj.toString(),(long)results.get(obj));
				if(results.get(obj) instanceof Double)
					tmp.put(obj.toString(),(double)results.get(obj));
				if(results.get(obj) instanceof Integer)
					tmp.put(obj.toString(),(int)results.get(obj));
			}
		}
		addDataToContainer(extra, tmp);
	}


	/**
	 * Creates a Statement connecting a the subject to the Task Resource using the iprop:workerResult property as follows
	 * ires:Task1 iprop:workerResult subject
	 * @param subject
	 * @return
	 */
	protected Statement getConnectingStatement(Resource subject) {
		return ResourceFactory.createStatement(getTaskResource(), Vocab.workerResult, subject);
	}

	public Resource getTaskResource(){
		String subject = metaData.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY);
		return ResourceFactory.createResource(COMMON.RES_BASE_URI+subject);
	}

	public Resource getSubject(Properties recv){
		String id = this.getSubjectFromExtraMeta(recv);
		return ResourceFactory.createResource(COMMON.RES_BASE_URI+id);
	}

	public Property getMetricProperty(){
		return ResourceFactory.createProperty(COMMON.PROP_BASE_URI+shortName);
	}

	public void sendData(Model m){
		this.storageManager.addData(m);
	}

	@Override
	public void close() {
		//Add metric description and worker class
		Model m = ModelFactory.createDefaultModel();
		String label = this.getClass().getCanonicalName();
		if(this.getClass().isAnnotationPresent(Shorthand.class)){
			label = getClass().getAnnotation(Shorthand.class).value();
		}
		Literal labelRes = ResourceFactory.createPlainLiteral(label);
		Literal commentRes = ResourceFactory.createPlainLiteral(this.description);
		Resource classRes = ResourceFactory.createResource(COMMON.CLASS_BASE_URI+"metric/"+label);
		Resource metricRes = ResourceFactory.createResource(COMMON.RES_BASE_URI+this.getShortName());
		//Resource metricClass = ResourceFactory.createResource(COMMON.CLASS_BASE_URI+this.getShortName());

		m.add(metricRes, RDFS.label, this.getName());
		m.add(metricRes, RDFS.comment, commentRes);
		//adding type iguana:metric
		m.add(metricRes, RDF.type, Vocab.metricClass);
		//adding type iguana:metric/SPECIFIC_METRIC_CLASS
		m.add(metricRes, RDF.type, classRes);
		m.add(metricRes, RDFS.label, labelRes);

		for(Properties key : dataContainer.keySet()) {

			Resource subject = ResourceFactory.createResource(COMMON.RES_BASE_URI+getSubjectFromExtraMeta(key));
			m.add(subject,
					RDF.type,
					Vocab.workerClass);
			for(Object k : key.keySet()) {
				Property prop = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + k);
				Object value = key.get(k);
				if (value instanceof Integer || value instanceof Long) {
					long long_value = ((Number) value).longValue();
					m.add(subject, prop, ResourceFactory.createTypedLiteral(BigInteger.valueOf(long_value)));
				}
				if (value instanceof Float || value instanceof Double) {
					double double_value = ((Number) value).doubleValue();
					m.add(subject, prop, ResourceFactory.createTypedLiteral(BigDecimal.valueOf(double_value)));
				} else
					m.add(subject, prop, ResourceFactory.createTypedLiteral(value));
			}
			m.add(subject, Vocab.worker2metric, metricRes);
		}
		m.add(getTaskResource(), Vocab.worker2metric, metricRes);

		this.storageManager.addData(m);

		this.dataContainer.clear();
	}
}
