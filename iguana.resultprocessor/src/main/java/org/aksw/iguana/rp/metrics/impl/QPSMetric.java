package org.aksw.iguana.rp.metrics.impl;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.data.Triple;
import org.aksw.iguana.rp.metrics.AbstractMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Queries Per Second Metric implementation
 * 
 * @author f.conrads
 *
 */
public class QPSMetric extends AbstractMetric {

	protected static Logger LOGGER = LoggerFactory.getLogger(QPSMetric.class);

	protected long hourInMS = 3600000;
	
	public QPSMetric() {
		super(
				"Queries Per Second",
				"QPS",
				"Will calculate for each query the amount of how many times the query could be executed succesfully in one second."
				+ "Further on it will save the totaltime of each query, the failure and the success");
	}

	@Override
	public void receiveData(Properties p) {
		//Save success and time of each query
		LOGGER.debug(this.getShortName() + " has received " + p);
		double time = Double.parseDouble(p.get(COMMON.RECEIVE_DATA_TIME).toString());
		long tmpSuccess = Long.parseLong(p.get(COMMON.RECEIVE_DATA_SUCCESS).toString());
		long success = tmpSuccess>0?1:0;
		long failure = success==1?0:1;
		long timeout = tmpSuccess==COMMON.SOCKET_TIMEOUT_VALUE?1:0;
		long unknown = tmpSuccess==COMMON.UNKNOWN_EXCEPTION_VALUE?1:0;
		long wrongCode = tmpSuccess==COMMON.WRONG_RESPONSE_CODE_VALUE?1:0;
		long size=-1;
		if(p.containsKey(COMMON.RECEIVE_DATA_SIZE)) {
			size = Long.parseLong(p.get(COMMON.RECEIVE_DATA_SIZE).toString());
		}
		String queryID = p.getProperty(COMMON.QUERY_ID_KEY);
		int queryHash = Integer.parseInt(p.get(COMMON.QUERY_HASH).toString());
		Properties extra = getExtraMeta(p);
		
		Properties tmp = getDataFromContainer(extra);
		if(tmp!=null && tmp.containsKey(queryID)){
			Object[] oldArr = (Object[]) tmp.get(queryID);
			oldArr[0] = (double) oldArr[0] + time;
			 oldArr[1] = (long) oldArr[1] + success;
			 oldArr[2] = (long) oldArr[2] + failure;
			if((long)oldArr[3]<size) {
				oldArr[3]=size;
			}
			oldArr[4] = (long) oldArr[4] + timeout;
			oldArr[5] = (long) oldArr[5] + unknown;
			oldArr[6] = (long) oldArr[6] + wrongCode;
		}
		else if(tmp!=null){
			Object[] resArr = {time, success, failure, size, timeout, unknown, wrongCode,queryHash};
			tmp.put(queryID, resArr);
		}
		else{
			tmp = new Properties();
			Object[] resArr = new Object[]{time, success, failure, size, timeout, unknown, wrongCode,queryHash};
			tmp.put(queryID, resArr);
		}
		addDataToContainer(extra, tmp);
		
	}

	@Override
	public void close() {
		//for each query/extra put {qID:amount} to properties
		for(Properties key : dataContainer.keySet()){
			Properties value = dataContainer.get(key);
			for(Object queryID : value.keySet()){
				Object[] resArr = (Object[]) value.get(queryID);
				Double qps = (long)resArr[1]*1.0/((double)resArr[0]/1000.0);
			
				//create Triple of results and use subject as object node
				String subject = getSubjectFromExtraMeta(key)+"/"+queryID;
				Set<String> isRes = new HashSet<String>();
				isRes.add(subject);

				Triple[] triples = new Triple[9];
				//qps
				triples[0] = new Triple(subject, "queriesPerSecond", qps);
				//failed
				triples[1] = new Triple(subject, "failed", (long)resArr[2]);
				//succeded
				triples[2] = new Triple(subject, "succeeded", (long)resArr[1]);

				//totaltime
				triples[3] = new Triple(subject, "totalTime", (double)resArr[0]);
				triples[4] = new Triple(subject, "resultSize", "?");
				if((long)resArr[3]!=-1L) {
					triples[4] = new Triple(subject, "resultSize", (long)resArr[3]);
				}
				triples[5] = new Triple(subject, "timeouts", (long)resArr[4]);
				triples[6] = new Triple(subject, "unknownExceptions", (long)resArr[5]);
				triples[7] = new Triple(subject, "wrongCodes", (long)resArr[6]);

				triples[8] = new Triple(subject, "queryID", (int)resArr[7] + "/" + queryID.toString());
				triples[8].setObjectResource(true);
				
				Properties results = new Properties();
				sendTriples(results, isRes, key, triples);
			}
		}
		super.close();
		
	}

}
