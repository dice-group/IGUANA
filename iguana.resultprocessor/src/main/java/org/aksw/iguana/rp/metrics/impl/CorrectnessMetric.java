package org.aksw.iguana.rp.metrics.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.data.Triple;
import org.aksw.iguana.rp.metrics.AbstractMetric;

@Deprecated
@Shorthand("Correctness")
public class CorrectnessMetric extends AbstractMetric {

	private static final int TP_RESULTS = 0;

	private static final int FP_RESULTS = 1;

	private static final int FN_RESULTS = 2;

	private static final int QUERY_ID = 0;

	private static final int QUERY_STRING = 1;

	private static final int DOUBLE_RESULTS = 2;

	protected String baseUri = "http://iguana-benchmark.eu";
	
	private Map<String, Object[]> rawResults = new HashMap<String, Object[]>();
	
	public CorrectnessMetric() {
		super("Correctness", "Correctness", "Will calculate the correctness using Micro and Macro F1 measure");
	}
	
	@Override
	public void receiveData(Properties p) {
		String queryID =  p.get(COMMON.QUERY_ID).toString();
		String queryString =  p.get(COMMON.QUERY_STRING).toString();
		double[] doubleResults =  (double[])p.get(COMMON.DOUBLE_RAW_RESULTS);
		Object[] rawResult = new Object[3];
		rawResult[QUERY_ID] = queryID;
		rawResult[QUERY_STRING] = queryString;
		rawResult[DOUBLE_RESULTS] = doubleResults;
		rawResults.put(queryID, rawResult);
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		String subject = getSubjectFromExtraMeta(new Properties());
		Triple[] triples = new Triple[rawResults.keySet().size()*9];
		double[] globalMeasure = new double[] {0,0,0};
		double[] globalRaw = new double[] {0,0,0};
		int i=0;
		for(String key : rawResults.keySet()) {
			Object[] rawResult = rawResults.get(key);
			String queryURI = subject+"/sparql"+rawResult[QUERY_ID].toString();
			triples[i] = new Triple(subject, "query", queryURI);
			triples[i++].setObjectResource(true);
			triples[i++] = new Triple(queryURI, "queryID", rawResult[QUERY_ID]);
			triples[i++] = new Triple(queryURI, "queryString", rawResult[QUERY_STRING].toString().replaceAll("(<|>)", ""));
			double[] rawDoubleResults = (double[])rawResult[DOUBLE_RESULTS];
			triples[i++] = new Triple(queryURI, "tp", rawDoubleResults[TP_RESULTS]);
			triples[i++] = new Triple(queryURI, "fp", rawDoubleResults[FP_RESULTS]);
			triples[i++] = new Triple(queryURI, "fn", rawDoubleResults[FN_RESULTS]);
			globalRaw[TP_RESULTS]+=rawDoubleResults[TP_RESULTS];
			globalRaw[FP_RESULTS]+=rawDoubleResults[FP_RESULTS];
			globalRaw[FN_RESULTS]+=rawDoubleResults[FN_RESULTS];
			double[] measure = calculateMeasure(rawDoubleResults);
			triples[i++] = new Triple(queryURI, "precision", measure[0]);
			triples[i++] = new Triple(queryURI, "recall", measure[1]);
			triples[i++] = new Triple(queryURI, "f1", measure[2]);
			globalMeasure[0] += measure[0];
			globalMeasure[1] += measure[1];
			globalMeasure[2] += measure[2];
		}
		Properties results = new Properties();
		double[] microMeasure = calculateMeasure(globalRaw);
		results.put("microPrecision", microMeasure[0]);
		results.put("microRecall", microMeasure[1]);
		results.put("microF1", microMeasure[2]);
		results.put("macroPrecision", globalMeasure[0]/rawResults.size());
		results.put("macroRecall", globalMeasure[1]/rawResults.size());
		results.put("macroF1", globalMeasure[2]/rawResults.size());
		sendTriples(subject, results, new HashSet<String>(), new Properties(),triples );
		super.close();
	}

	private double[] calculateMeasure(double[] rawDoubleResults) {
		double[] measure = new double[] {0,0,0};
		double tp = rawDoubleResults[TP_RESULTS];
		double fp = rawDoubleResults[FP_RESULTS];
		double fn = rawDoubleResults[FN_RESULTS];
		if(tp==0&&fp==0&&fn==0) {
			return new double[]{1,1,1};
		}
		if(fp!=0||tp!=0) {
			measure[0] = tp/(tp+fp);			
		}
		if(fp!=0||tp!=0) {
			measure[1] = tp/(tp+fn);
		}
		if(measure[0]!=0 || measure[1]!=0)
			measure[2] = 2*measure[0]*measure[1]/(measure[0]+measure[1]);
		return measure;
	}

}
