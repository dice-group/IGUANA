/**
 * 
 */
package org.aksw.iguana.rp.metrics.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.data.Triple;
import org.aksw.iguana.rp.metrics.AbstractMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * This metric will send every query execution time to the storages. Also it
 * will provide if the query succeded or failed.
 * 
 * @author f.conrads
 *
 */
public class EachQueryMetric extends AbstractMetric {

	private Map<String, Long> queryRunMap = new HashMap<String, Long>();
	
	protected static Logger LOGGER = LoggerFactory
			.getLogger(EachQueryMetric.class);

	/**
	 * 
	 */
	public EachQueryMetric() {
		super("Each Query Execution", "EQE",
				"Will calculate every query execution time.");
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
		
		String subject = getSubjectFromExtraMeta(p);
//		Properties extraMeta = getExtraMeta(p);
//		if (!extraMeta.isEmpty()) {
//			subject += "/" + extraMeta.hashCode();
//		}
		
		LOGGER.debug(this.getShortName() + " has received " + p);

		double time = (double) p.get(COMMON.RECEIVE_DATA_TIME);
		Boolean success = (Boolean) (((long) p.get(COMMON.RECEIVE_DATA_SUCCESS))>0?true:false);
		String queryID = p.getProperty(COMMON.QUERY_ID_KEY);
		long err = (long) p.get(COMMON.RECEIVE_DATA_SUCCESS);
		subject += "/"+queryID;

		long run=1;
		if(queryRunMap.containsKey(subject)){
			run = queryRunMap.get(subject)+1;
		}
		//set subject2 node subject/noOfRun
		String subject2 = subject+"/"+run;
		Properties results = new Properties();
		results.put("EQE", subject2);

		//as triples
		Triple[] triples = new Triple[5];
		triples[0] = new Triple(subject2, "time", time);
		triples[1] = new Triple(subject2, "success", success);
		triples[2] = new Triple(subject2, "queryID", queryID);
		triples[3] = new Triple(subject2, "run", run);
		triples[4] = new Triple(subject2, "errorCode", err);

		
		Set<String> isRes = new HashSet<String>();
		isRes.add(queryID);
	
		sendTriples(subject, results, isRes, p, triples);
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
