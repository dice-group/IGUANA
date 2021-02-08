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

import java.math.BigDecimal;
import java.util.Properties;

/**
 * 
 * The Number Of Queries Per Hour Metric 
 * 
 * @author f.conrads
 *
 */
@Shorthand("NoQPH")
public class NoQPHMetric extends AbstractMetric {

	protected static final Object TOTAL_TIME = "totalTime";
	protected static final Object TOTAL_SUCCESS = "totalSuccess";


	protected static Logger LOGGER = LoggerFactory.getLogger(NoQPHMetric.class);

	
	protected long hourInMS = 3600000;
	
	
	public NoQPHMetric(){
		super("Number Of Queries Per Hour", "NoQPH", "Will calculate the number of queries which could be executed successfully per Hour.");
	}
	
	protected NoQPHMetric(String name, String shortName, String description){
		super(name, shortName, description);
	}
	
	
	/* (non-Javadoc)
	 * @see org.aksw.iguana.rp.metrics.Metric#receiveData(java.util.Properties)
	 */
	@Override
	public void receiveData(Properties p) {
		LOGGER.debug(this.getShortName()+" has received "+p);
		double time = Double.parseDouble(p.get(COMMON.RECEIVE_DATA_TIME).toString());
		Integer success = (long)p.get(COMMON.RECEIVE_DATA_SUCCESS)>0?1:0;	
		
		Properties results = new Properties();
		results.put(TOTAL_TIME, time);
		results.put(TOTAL_SUCCESS, success);
		
		Properties extra = getExtraMeta(p);
		processData(extra, results);
	}

	/* (non-Javadoc)
	 * @see org.aksw.iguana.rp.metrics.Metric#close()
	 */
	@Override
	public void close() {
		callbackClose();
		super.close();
		
	}

	protected void callbackClose() {
		Model m = ModelFactory.createDefaultModel();
		Property property = getMetricProperty();
		double sum = 0.0;
		for(Properties key : dataContainer.keySet()){
			Double totalTime = (Double) dataContainer.get(key).get(TOTAL_TIME);
			Integer success = (Integer) dataContainer.get(key).get(TOTAL_SUCCESS);
			double noOfQueriesPerHour = hourInMS*success*1.0/totalTime;
			sum+=noOfQueriesPerHour;
			Resource subject = getSubject(key);
			m.add(getConnectingStatement(subject));
			m.add(subject, property, ResourceFactory.createTypedLiteral(BigDecimal.valueOf(noOfQueriesPerHour)));
		}

		m.add(getTaskResource(), property, ResourceFactory.createTypedLiteral(BigDecimal.valueOf(sum)));
		sendData(m);
	}



	}
