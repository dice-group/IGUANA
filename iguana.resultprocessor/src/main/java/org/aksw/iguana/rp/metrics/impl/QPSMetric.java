package org.aksw.iguana.rp.metrics.impl;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.data.Triple;
import org.aksw.iguana.rp.metrics.AbstractMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		long time = Long.parseLong(p.get(COMMON.RECEIVE_DATA_TIME).toString());
		long success = (Boolean) p.get(COMMON.RECEIVE_DATA_SUCCESS)?1:0;
		long failure = success==1?0:1;
		String queryID = p.getProperty(COMMON.QUERY_ID_KEY);
		
		Properties extra = getExtraMeta(p);
		
		Properties tmp = getDataFromContainer(extra);
		if(tmp!=null && tmp.containsKey(queryID)){
			long[] oldArr = (long[])tmp.get(queryID);
			oldArr[0]+=time;
			oldArr[1]+=success;
			oldArr[2]+=failure;
		}
		else if(tmp!=null){
			long[] resArr = {time, success, failure};
			tmp.put(queryID, resArr);
		}
		else{
			tmp = new Properties();
			long[] resArr = new long[]{time, success, failure};
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
				long[] resArr = (long[]) value.get(queryID);
				Double qps = resArr[1]*1.0/(resArr[0]/1000.0);
			
				//create Triple of results and use subject as object node
				String subject = getSubjectFromExtraMeta(key)+"/"+queryID;
				Set<String> isRes = new HashSet<String>();
				isRes.add(subject);
				
				Triple[] triples = new Triple[5];
				//qps
				triples[0] = new Triple(subject, "queriesPerSecond", qps);
				//failed
				triples[1] = new Triple(subject, "failed", resArr[2]);
				//succeded
				triples[2] = new Triple(subject, "succeded", resArr[1]);
				//queryID
				triples[3] = new Triple(subject, "queryID", queryID.toString());
				triples[3].setObjectResource(true);
				//totaltime
				triples[4] = new Triple(subject, "totalTime", resArr[0]);
				
				Properties results = new Properties();
				results.put("qps#query", subject);
				sendTriples(results, isRes, key, triples);
			}
		}
	}

}
