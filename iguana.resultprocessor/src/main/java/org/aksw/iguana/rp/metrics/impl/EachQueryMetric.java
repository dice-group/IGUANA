/**
 * 
 */
package org.aksw.iguana.rp.metrics.impl;

import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.metrics.AbstractMetric;
import org.aksw.iguana.rp.vocab.Vocab;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
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
		Boolean success = ((long) p.get(COMMON.RECEIVE_DATA_SUCCESS))>0;
		String fullQueryId = p.getProperty(COMMON.FULL_QUERY_ID_KEY);
		long err = (long) p.get(COMMON.RECEIVE_DATA_SUCCESS);
		String subject = worker+"/"+fullQueryId;

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
		m.add(subRes, timeProperty, ResourceFactory.createTypedLiteral(BigDecimal.valueOf(time)));
		m.add(subRes, successProperty, ResourceFactory.createTypedLiteral(success));
		if(p.containsKey(COMMON.QUERY_HASH)) {
			int queryHash = Integer.parseInt(p.get(COMMON.QUERY_HASH).toString());
			m.add(subRes, Vocab.queryProp, ResourceFactory.createResource(COMMON.RES_BASE_URI+queryHash+"/"+fullQueryId));
		}
		else{
			// TODO: when may this ever happen?
			m.add(subRes, queryIDProperty, ResourceFactory.createTypedLiteral(fullQueryId));
		}
		m.add(subRes, runProperty, ResourceFactory.createTypedLiteral(BigInteger.valueOf(run)));
		m.add(subRes, errorCodeProperty, ResourceFactory.createTypedLiteral(BigInteger.valueOf(err)));
		if(p.containsKey(COMMON.RECEIVE_DATA_SIZE)) {
			long resSize = Long.parseLong(p.get(COMMON.RECEIVE_DATA_SIZE).toString());
			m.add(subRes, resultSize, ResourceFactory.createTypedLiteral(BigInteger.valueOf(resSize)));
		}

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
