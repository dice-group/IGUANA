/**
 * 
 */
package org.aksw.iguana.rp.metrics.impl;

import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.metrics.AbstractMetric;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * This metric will send every query execution time to the storages. Also it
 * will provide if the query succeeded or failed.
 * 
 * @author f.conrads
 *
 */
@Shorthand("EachQuery")
public class EachQueryMetric extends AbstractMetric {

	private static Property queryProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"query");
	private static Property execProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"queryExecution");
	private static Property resultSize = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"resultSize");
	private static Property timeProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"time");
	private static Property successProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"success");
	private static Property runProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"run");
	private static Property queryIDProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"queryID");
	private static Property errorCodeProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"code");


	private Map<String, Long> queryRunMap = new HashMap<String, Long>();
	
	protected static Logger LOGGER = LoggerFactory
			.getLogger(EachQueryMetric.class);

	/**
	 * 
	 */
	public EachQueryMetric() {
		super("Each Query Execution", "EachQuery",
				"Will save every query execution time.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aksw.iguana.rp.metrics.Metric#receiveData(java.util.Properties)
	 */
	@Override
	public void receiveData(Properties p) {
		// set Subject Node, hash out of task ID and if not empty the extra
		// properties
		Model m = ModelFactory.createDefaultModel();
		
		String worker = getSubjectFromExtraMeta((Properties) p.get(COMMON.EXTRA_META_KEY));

		
		LOGGER.debug(this.getShortName() + " has received " + p);

		double time = (double) p.get(COMMON.RECEIVE_DATA_TIME);
		Boolean success = (Boolean) (((long) p.get(COMMON.RECEIVE_DATA_SUCCESS))>0?true:false);
		String queryID = p.getProperty(COMMON.QUERY_ID_KEY);
		long err = (long) p.get(COMMON.RECEIVE_DATA_SUCCESS);
		String subject = worker+"/"+queryID;

		long run=1;
		if(queryRunMap.containsKey(subject)){
			run = queryRunMap.get(subject)+1;
		}
		//set subject2 node subject/noOfRun
		String subject2 = subject+"/"+run;

		//as triples
		Resource workerRes = ResourceFactory.createResource(COMMON.RES_BASE_URI+worker);

		Resource queryRes = ResourceFactory.createResource(COMMON.RES_BASE_URI+subject);

		Resource subRes = ResourceFactory.createResource(COMMON.RES_BASE_URI+subject2);
		m.add(getConnectingStatement(workerRes));
		m.add(workerRes, queryProperty , queryRes);
		m.add(queryRes, execProperty , subRes);
		m.add(subRes, timeProperty, ResourceFactory.createTypedLiteral(time));
		m.add(subRes, successProperty, ResourceFactory.createTypedLiteral(success));
		if(p.containsKey(COMMON.QUERY_HASH)) {
			int queryHash = Integer.parseInt(p.get(COMMON.QUERY_HASH).toString());
			m.add(subRes, queryIDProperty, ResourceFactory.createTypedLiteral(COMMON.RES_BASE_URI+queryHash+"/"+queryID));
		}
		else{
			m.add(subRes, queryIDProperty, ResourceFactory.createTypedLiteral(queryID));
		}
		m.add(subRes, runProperty, ResourceFactory.createTypedLiteral(run));
		m.add(subRes, errorCodeProperty, ResourceFactory.createTypedLiteral(err));
		m.add(subRes, resultSize, ResourceFactory.createTypedLiteral(p.get(COMMON.RECEIVE_DATA_SIZE)));

		sendData(m);
		queryRunMap.put(subject, run);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aksw.iguana.rp.metrics.Metric#close()
	 */
	@Override
	public void close() {
		// Nothing to do here, as each query was sent to the Storages yet.
		super.close();
	}

}
