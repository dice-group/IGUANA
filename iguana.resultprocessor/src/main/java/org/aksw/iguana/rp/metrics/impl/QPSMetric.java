package org.aksw.iguana.rp.metrics.impl;

import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.metrics.AbstractMetric;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Queries Per Second Metric implementation
 * 
 * @author f.conrads
 *
 */
@Shorthand("QPS")
public class QPSMetric extends AbstractMetric {

	protected static Logger LOGGER = LoggerFactory.getLogger(QPSMetric.class);

	private static Property queryProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"query");
	private static Property failProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"failed");
	private static Property succeededProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"succeeded");
	private static Property ttProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"totalTime");
	private static Property resultSize = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"resultSize");
	private static Property timeOuts = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"timeOuts");
	private static Property unknownException = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"unknownException");
	private static Property wrongCodes = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"wrongCodes");
	private static Property penalizedQPSProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"penalizedQPS");
	private static Property query = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"query");

	protected long hourInMS = 3600000;
	protected Integer penalty = null;
	private boolean noPenalty= false;

	public QPSMetric() {
		super(
				"Queries Per Second",
				"QPS",
				"Will calculate for each query the amount of how many times the query could be executed succesfully in one second."
				+ " Further on it will save the totaltime of each query, the failure and the success");
	}

	public QPSMetric(Integer penalty) {
		super(
				"Queries Per Second",
				"QPS",
				"Will calculate for each query the amount of how many times the query could be executed succesfully in one second."
						+ " Further on it will save the totaltime of each query, the failure and the success");
		this.penalty = penalty;
	}

	public QPSMetric(String name, String shortName, String description) {
		super(name, shortName, description);
	}

	@Override
	public void receiveData(Properties p) {
		//Save success and time of each query
		LOGGER.debug(this.getShortName() + " has received " + p);
		double time = Double.parseDouble(p.get(COMMON.RECEIVE_DATA_TIME).toString());
		long tmpSuccess = Long.parseLong(p.get(COMMON.RECEIVE_DATA_SUCCESS).toString());
		long success = tmpSuccess>0?1:0;
		long failure = success==1?0:1;
		long timeout = tmpSuccess==COMMON.QUERY_SOCKET_TIMEOUT?1:0;
		long unknown = tmpSuccess==COMMON.QUERY_UNKNOWN_EXCEPTION?1:0;
		long wrongCode = tmpSuccess==COMMON.QUERY_HTTP_FAILURE?1:0;
		Double penalty=getPenalty(p);

		long size=-1;
		double penalizedTime=getPenalizedTime(penalty, failure, time);
		if(p.containsKey(COMMON.RECEIVE_DATA_SIZE)) {
			size = Long.parseLong(p.get(COMMON.RECEIVE_DATA_SIZE).toString());
		}
		String queryID = p.getProperty(COMMON.FULL_QUERY_ID_KEY);
		int queryHash = Integer.parseInt(p.get(COMMON.QUERY_HASH).toString());
		Properties extra = getExtraMeta(p);
		
		Properties tmp = putResults(extra, time, success, failure, timeout, unknown, wrongCode, penalizedTime, size, queryHash, queryID);
		addDataToContainer(extra, tmp);
		
	}

	private Properties putResults(Properties extra, double time, long success, long failure, long timeout, long unknown, long wrongCode, double penalizedTime, long size, int queryHash, String queryID) {
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
			oldArr[7] = (double) oldArr[7] + penalizedTime;
		}
		else if(tmp!=null){
			Object[] resArr = {time, success, failure, size, timeout, unknown, wrongCode, penalizedTime, queryHash};
			tmp.put(queryID, resArr);
		}
		else{
			tmp = new Properties();
			Object[] resArr = new Object[]{time, success, failure, size, timeout, unknown, wrongCode,penalizedTime,queryHash};
			tmp.put(queryID, resArr);
		}
		return tmp;
	}


	private double getPenalizedTime(Double penalty, long failure, double time) {
		if(failure==1){
			if(this.penalty!=null) {
				return this.penalty;
			}
			else if(penalty!=null){
				//use task provided penalty
				return penalty;
			}
			else{
				LOGGER.error("Penalty was neither set in Task nor Config. penaltyQPS will show not be included.");
				this.noPenalty=true;
			}
		}
		return time;
	}

	private Double getPenalty(Properties p) {
		try {
			if(p.containsKey(COMMON.PENALTY)) {
				return Double.parseDouble(p.get(COMMON.PENALTY).toString());
			}
		}catch(Exception e){
			LOGGER.warn("Penalty could not be set. Error: {}", e);
		}
		return null;
	}

	@Override
	public void close() {
		qpsClose();
		super.close();
		
	}

	/**
	 * Callback method which will be used in close
	 */
	protected void qpsClose() {
		//for each query/extra put {qID:amount} to properties
		Map<Object, Object> map = new HashMap<Object, Object>();
		Model m = ModelFactory.createDefaultModel();

		for(Properties key : dataContainer.keySet()){
			Properties value = dataContainer.get(key);
			Resource subjectParent = getSubject(key);
			m.add(getConnectingStatement(subjectParent));
			addToModel(value, subjectParent, m, map);
		}
		Resource subjectParent = getTaskResource();
		addToModel( map, subjectParent, m, null);
		sendData(m);
	}

	private void addToModel(Map<Object, Object> value, Resource subjectParent, Model m, Map<Object, Object> map){
		Property qpsProperty = getMetricProperty();

		for(Object queryID : value.keySet()){
			Object[] resArr = (Object[]) value.get(queryID);
			if(map!=null)
				mergeResults(map, queryID, resArr);
			Double qps = (long)resArr[1]*1.0/((double)resArr[0]/1000.0);
			Double pqps = (long)resArr[1]*1.0/((double)resArr[7]/1000.0);

			Resource query = ResourceFactory.createResource(subjectParent.getURI()+"/"+queryID);
			m.add(subjectParent, queryProperty, query);
			m.add(query, qpsProperty, ResourceFactory.createTypedLiteral(BigDecimal.valueOf(qps)));
			m.add(query, ttProperty, ResourceFactory.createTypedLiteral(BigDecimal.valueOf((double)resArr[0])));
			m.add(query, succeededProperty, ResourceFactory.createTypedLiteral(BigInteger.valueOf((long) resArr[1])));
			m.add(query, failProperty, ResourceFactory.createTypedLiteral(BigInteger.valueOf((long) resArr[2])));
			if((long)resArr[3]!=-1L) {
				m.add(query, resultSize, ResourceFactory.createTypedLiteral(BigInteger.valueOf((long) resArr[3])));
			}
			else{
				m.add(query, resultSize, ResourceFactory.createTypedLiteral("?"));
			}
			m.add(query, timeOuts, ResourceFactory.createTypedLiteral(BigInteger.valueOf((long)resArr[4])));
			m.add(query, unknownException, ResourceFactory.createTypedLiteral(BigInteger.valueOf((long)resArr[5])));
			m.add(query, wrongCodes, ResourceFactory.createTypedLiteral(BigInteger.valueOf((long)resArr[6])));
			if(!noPenalty) {
				m.add(query, penalizedQPSProperty, ResourceFactory.createTypedLiteral(BigDecimal.valueOf(pqps)));
			}
			m.add(query, QPSMetric.query, ResourceFactory.createResource(COMMON.RES_BASE_URI+(int)resArr[8]+ "/" + queryID.toString()));
			m.add(query, RDF.type, ResourceFactory.createResource(COMMON.CLASS_BASE_URI+"ExecutedQuery"));
		}
	}

	private void mergeResults(Map<Object, Object> map, Object queryID, Object[] resArr) {
		if(map.containsKey(queryID)){
			Object[] currentResults = (Object[])map.get(queryID);
			Object[] newResults = new Object[currentResults.length];
			for(int i=0;i<currentResults.length;i++){
				if(i==3 || i==8){
					//Result Size & query Hash doesn't make sense to sum up
					newResults[i]=resArr[i];
				}
				else if(currentResults[i] instanceof Long){
					newResults[i] = ((Number)currentResults[i]).longValue()+((Number)resArr[i]).longValue();
				}
				else if(currentResults[i] instanceof Integer) {
					newResults[i] = ((Number)currentResults[i]).intValue()+((Number)resArr[i]).intValue();
				}
				else{
					//assume Double
					newResults[i] = ((Number)currentResults[i]).doubleValue()+((Number)resArr[i]).doubleValue();
				}
			}
			map.put(queryID, newResults);
		}
		else{
			map.put(queryID, resArr);
		}
	}

}
