package org.aksw.iguana.rp.metrics.impl;

import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.metrics.AbstractMetric;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
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
	private static Property queryID = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"queryID");

	private static Property meanTime = ResourceFactory.createProperty(COMMON.PROP_WIKIBASE_URI+"meanQueryTime");
	private static Property penalizedMeanTime = ResourceFactory.createProperty(COMMON.PROP_WIKIBASE_URI+"penalizedMeanQueryTime");
	private static Property geometricMeanTime= ResourceFactory.createProperty(COMMON.PROP_WIKIBASE_URI+"geometricMeanQueryTime");
	private static Property penalizedGeometricMeanTime = ResourceFactory.createProperty(COMMON.PROP_WIKIBASE_URI+"penalizedGeometricMeanQueryTime");
	private static Property minTime = ResourceFactory.createProperty(COMMON.PROP_WIKIBASE_URI+"minQueryTime");
	private static Property maxTime = ResourceFactory.createProperty(COMMON.PROP_WIKIBASE_URI+"maxQueryTime");

	protected Integer penalty = null;
	private boolean noPenalty= false;

	private double minMaxDefault = -1.0;
	private double zero = 0.0;
	private double one = 1.0;

	public QPSMetric() {
		super(
				"Queries Per Second",
				"QPS",
				"Will calculate for each query the number of times that the query could be executed successfully in one second."
						+ " It will save the total, min and max execution times of each query, the number of failures and successes, and other details.");
	}

	public QPSMetric(Integer penalty) {
		super(
				"Queries Per Second",
				"QPS",
				"Will calculate for each query the number of times that the query could be executed successfully in one second."
						+ " It will save the total, min and max execution times of each query, the number of failures and successes, and other details.");
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
		String queryID = p.getProperty(COMMON.QUERY_ID_KEY);
		int queryHash = Integer.parseInt(p.get(COMMON.QUERY_HASH).toString());
		Properties extra = getExtraMeta(p);
		
		Properties tmp = putResults(extra, time, success, failure, timeout, unknown, wrongCode,
				penalizedTime, size, queryHash, queryID);
		addDataToContainer(extra, tmp);
		
	}

	private Properties putResults(Properties extra, double time, long success, long failure, long timeout,
								  long unknown, long wrongCode, double penalizedTime, long size,
								  int queryHash, String queryID) {
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
			// oldArr[8] is query hash
			if(success>0L) {
				if ((double) oldArr[9] < 0 || (double) oldArr[9] > time) {   // Min time
					oldArr[9] = time;
				}
				if ((double) oldArr[10] < 0 || (double) oldArr[10] < time) {   // Max time
					oldArr[10] = time;
				}
				oldArr[11] = (double) oldArr[11] + time;   // For mean
				oldArr[12] = (double) oldArr[12] + (double)Math.log(time);   // For geometric mean
				oldArr[13] = (double) oldArr[13] + penalizedTime;   // For penalized mean
				oldArr[14] = (double) oldArr[14] + (double)Math.log(penalizedTime);   // For penalized geometric mean
			}
			return tmp;
		}
		if(tmp==null) {
			tmp = new Properties();
		}
		if (success>0L) {
			Object[] resArr = new Object[]{time, success, failure, size, timeout, unknown, wrongCode,
					penalizedTime, queryHash, time, time, time, (double)Math.log(time), penalizedTime,
					(double)Math.log(penalizedTime)};
			tmp.put(queryID, resArr);
		}
		else {
			Object[] resArr = new Object[]{time, success, failure, size, timeout, unknown, wrongCode,
					penalizedTime, queryHash, minMaxDefault, minMaxDefault, zero, zero, zero, zero};
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
				LOGGER.error("Penalty was neither set in Task nor Config. penaltyQPS will not be included.");
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
			double qps = (long)resArr[1]*one/((double)resArr[0]/1000.0);
			double pqps = (long)resArr[1]*one/((double)resArr[7]/1000.0);

			long succeeded = (long)resArr[1];
			double mean = zero;   // Define variables outside the if clause for use later
			double geomMean = zero;
			double pMean = zero;
			double pGeomMean = zero;
			if (succeeded>0L){
				mean = (double)resArr[11]/succeeded;
				geomMean = (double)Math.exp((double)resArr[12]/succeeded);
				pMean = (double)resArr[13]/succeeded;
				pGeomMean = (double)Math.exp((double)resArr[14]/succeeded);
			}

			Resource query = ResourceFactory.createResource(subjectParent.getURI()+"/"+queryID);
			m.add(subjectParent, queryProperty, query);
			BigDecimal qpsBD = new BigDecimal(String.valueOf(qps));    // Want decimal literal
			m.add(query, qpsProperty, ResourceFactory.createTypedLiteral(qpsBD));
			BigDecimal timeBD = new BigDecimal(String.valueOf(resArr[0]));
			m.add(query, ttProperty, ResourceFactory.createTypedLiteral(timeBD));
			m.add(query, succeededProperty, ResourceFactory.createTypedLiteral(resArr[1]));
			m.add(query, failProperty, ResourceFactory.createTypedLiteral(resArr[2]));
			if((long)resArr[3] != -1L) {
				m.add(query, resultSize, ResourceFactory.createTypedLiteral(resArr[3]));
			}
			else{
				m.add(query, resultSize, ResourceFactory.createTypedLiteral("?"));
			}
			if((double)resArr[9]>zero) {
				BigDecimal minBD = new BigDecimal(String.valueOf(resArr[9]));
				m.add(query, minTime, ResourceFactory.createTypedLiteral(minBD));
			}
			else{
				m.add(query, minTime, ResourceFactory.createTypedLiteral("?"));
			}
			if((double)resArr[10]>zero) {
				BigDecimal maxBD = new BigDecimal(String.valueOf(resArr[10]));
				m.add(query, maxTime, ResourceFactory.createTypedLiteral(maxBD));
			}
			else{
				m.add(query, maxTime, ResourceFactory.createTypedLiteral("?"));
			}
			m.add(query, timeOuts, ResourceFactory.createTypedLiteral(resArr[4]));
			m.add(query, unknownException, ResourceFactory.createTypedLiteral(resArr[5]));
			m.add(query, wrongCodes, ResourceFactory.createTypedLiteral(resArr[6]));
			if (succeeded>0L){
				BigDecimal meanBD = new BigDecimal(String.valueOf(mean));
				m.add(query, meanTime, ResourceFactory.createTypedLiteral(meanBD));
				BigDecimal gMeanBD = new BigDecimal(String.valueOf(geomMean));
				float fgMean = gMeanBD.floatValue();   // Convert to float to avoid results like 149.999999998
				BigDecimal fgMeanBD = new BigDecimal(String.valueOf(fgMean));
				m.add(query, geometricMeanTime, ResourceFactory.createTypedLiteral(fgMeanBD));
			}
			else{
				m.add(query, meanTime, ResourceFactory.createTypedLiteral("?"));
				m.add(query, geometricMeanTime, ResourceFactory.createTypedLiteral("?"));
			}
			if(!noPenalty) {
				try {
					BigDecimal pQpsBD = new BigDecimal(String.valueOf(pqps));
					m.add(query, penalizedQPSProperty, ResourceFactory.createTypedLiteral(pQpsBD));
				} catch (NumberFormatException e) {
					String pQpsStr = "penal.qps." + String.valueOf(pqps);
					m.add(query, penalizedQPSProperty, ResourceFactory.createTypedLiteral(pQpsStr));
				}
				if (succeeded>0L){
					BigDecimal pMeanBD = new BigDecimal(String.valueOf(pMean));
					m.add(query, penalizedMeanTime, ResourceFactory.createTypedLiteral(pMeanBD));
					BigDecimal pgMeanBD = new BigDecimal(String.valueOf(pGeomMean));
					float fpgMean = pgMeanBD.floatValue();
					BigDecimal fpgMeanBD = new BigDecimal(String.valueOf(fpgMean));
					m.add(query, penalizedGeometricMeanTime, ResourceFactory.createTypedLiteral(fpgMeanBD));
				}
				else{
					m.add(query, penalizedMeanTime, ResourceFactory.createTypedLiteral("?"));
					m.add(query, penalizedGeometricMeanTime, ResourceFactory.createTypedLiteral("?"));
				}
			}
			m.add(query, QPSMetric.queryID, ResourceFactory.createResource(COMMON.RES_BASE_URI+(int)resArr[8]+ "/" + queryID.toString()));
			m.add(query, RDF.type, ResourceFactory.createResource(COMMON.CLASS_BASE_URI+"ExecutedQuery"));
		}
	}

	private void mergeResults(Map<Object, Object> map, Object queryID, Object[] resArr) {
		if(map.containsKey(queryID)){
			Object[] currentResults = (Object[])map.get(queryID);
			Object[] newResults = new Object[currentResults.length];
			for(int i=0;i<currentResults.length;i++){
				if(i==3 || i==8){
					// Result size and query hash don't need to be summed
					newResults[i] = resArr[i];
				}
				else if(i==9){
					// Min time doesn't sum but needs checking
					if(((Number)resArr[9]).doubleValue()>zero){
						if(((Number)currentResults[9]).doubleValue()<zero ||
								((Number)resArr[9]).doubleValue()<((Number)currentResults[9]).doubleValue()){
							newResults[9] = resArr[9];
						}
						else{
							newResults[9] = currentResults[9];
						}
					}
					else{
						newResults[9] = currentResults[9];
					}
				}
				else if(i==10){
					// Max time doesn't sum but needs checking
					if(((Number)resArr[10]).doubleValue()>zero){
						if(((Number)currentResults[10]).doubleValue()<zero ||
								((Number)resArr[10]).doubleValue()>((Number)currentResults[9]).doubleValue()){
							newResults[10] = resArr[10];
						}
						else{
							newResults[10] = currentResults[10];
						}
					}
					else{
						newResults[10] = currentResults[10];
					}
				}
				else if(i==12 || i==14){
					// Logarithm of the times are added to obtain the geometric mean
					newResults[i] = ((Number)currentResults[i]).doubleValue() + ((Number)resArr[i]).doubleValue();
				}
				else if(currentResults[i] instanceof Long){
					newResults[i] = ((Number)currentResults[i]).longValue() + ((Number)resArr[i]).longValue();
				}
				else if(currentResults[i] instanceof Integer) {
					newResults[i] = ((Number)currentResults[i]).intValue() + ((Number)resArr[i]).intValue();
				}
				else{
					// assume Double
					newResults[i] = ((Number)currentResults[i]).doubleValue() + ((Number)resArr[i]).doubleValue();
				}
			}
			map.put(queryID, newResults);
		}
		else{
			map.put(queryID, resArr);
		}
	}
}
