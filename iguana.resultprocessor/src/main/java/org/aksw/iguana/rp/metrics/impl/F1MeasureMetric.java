package org.aksw.iguana.rp.metrics.impl;

import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.metrics.AbstractMetric;
import org.apache.jena.rdf.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * provides a metric to measure F1, recall and precision if provided tp,fp,fn.
 * Calculates micro and macro f1, recall and precision as well.
 */
@Shorthand("F1Measure")
public class F1MeasureMetric extends AbstractMetric {
	// TODO: check output
	private static Property queryProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"query");
	private static Property queryIDProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"queryID");
	private static Property queryStringProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"queryString");
	private static Property tpProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"tp");
	private static Property fpProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"fp");
	private static Property fnProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"fn");
	private static Property precisionProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"precision");
	private static Property recallProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"recall");
	private static Property f1Property = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"f1");
	private static Property microPrecisionProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"microPrecision");
	private static Property microRecallProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"microRecall");
	private static Property microF1Property = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"microF1");
	private static Property macroPrecisionProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"macroPrecision");
	private static Property macroRecallProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"macroRecall");
	private static Property macroF1Property = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"macroF1");


	private static final int TP_RESULTS = 0;

	private static final int FP_RESULTS = 1;

	private static final int FN_RESULTS = 2;

	private static final int QUERY_ID = 0;

	private static final int QUERY_STRING = 1;

	private static final int DOUBLE_RESULTS = 2;

	private Map<String, Object[]> rawResults = new HashMap<String, Object[]>();
	
	public F1MeasureMetric() {
		super("F1 Measure", "F1Measure", "Will calculate Micro and Macro F1 measure");
	}
	
	@Override
	public void receiveData(Properties p) {
		String queryID =  p.get(COMMON.FULL_QUERY_ID_KEY).toString();
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
		String subject = getSubjectFromExtraMeta(new Properties());

		Model m = ModelFactory.createDefaultModel();
		Resource subRes= ResourceFactory.createResource(COMMON.RES_BASE_URI+subject);

		double[] globalMeasure = new double[] {0,0,0};
		double[] globalRaw = new double[] {0,0,0};
		int i=0;
		for(String key : rawResults.keySet()) {
			Object[] rawResult = rawResults.get(key);
			String queryURI = COMMON.RES_BASE_URI+subject+"/"+rawResult[QUERY_ID].toString();
			Resource queryURIRes = ResourceFactory.createResource(queryURI);
			m.add(subRes, queryProperty, queryURIRes);
			m.add(queryURIRes, queryIDProperty, ResourceFactory.createTypedLiteral(rawResult[QUERY_ID]));
			m.add(queryURIRes, queryStringProperty, ResourceFactory.createTypedLiteral(rawResult[QUERY_STRING].toString().replaceAll("(<|>)", "")));

			double[] rawDoubleResults = (double[])rawResult[DOUBLE_RESULTS];
			m.add(queryURIRes, tpProperty, ResourceFactory.createTypedLiteral(rawDoubleResults[TP_RESULTS]));
			m.add(queryURIRes, fpProperty, ResourceFactory.createTypedLiteral(rawDoubleResults[FP_RESULTS]));
			m.add(queryURIRes, fnProperty, ResourceFactory.createTypedLiteral(rawDoubleResults[FN_RESULTS]));

			globalRaw[TP_RESULTS]+=rawDoubleResults[TP_RESULTS];
			globalRaw[FP_RESULTS]+=rawDoubleResults[FP_RESULTS];
			globalRaw[FN_RESULTS]+=rawDoubleResults[FN_RESULTS];
			double[] measure = calculateMeasure(rawDoubleResults);
			m.add(queryURIRes, precisionProperty, ResourceFactory.createTypedLiteral(measure[0]));
			m.add(queryURIRes, recallProperty, ResourceFactory.createTypedLiteral(measure[1]));
			m.add(queryURIRes, f1Property, ResourceFactory.createTypedLiteral(measure[2]));

			globalMeasure[0] += measure[0];
			globalMeasure[1] += measure[1];
			globalMeasure[2] += measure[2];
		}
		Properties results = new Properties();
		double[] microMeasure = calculateMeasure(globalRaw);
		m.add(subRes, microPrecisionProperty, ResourceFactory.createTypedLiteral(microMeasure[0]));
		m.add(subRes, microRecallProperty, ResourceFactory.createTypedLiteral(microMeasure[1]));
		m.add(subRes, microF1Property, ResourceFactory.createTypedLiteral(microMeasure[2]));
		m.add(subRes, macroPrecisionProperty, ResourceFactory.createTypedLiteral(globalMeasure[0]/rawResults.size()));
		m.add(subRes, macroRecallProperty, ResourceFactory.createTypedLiteral(globalMeasure[1]/rawResults.size()));
		m.add(subRes, macroF1Property, ResourceFactory.createTypedLiteral(globalMeasure[2]/rawResults.size()));
		sendData(m);
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
