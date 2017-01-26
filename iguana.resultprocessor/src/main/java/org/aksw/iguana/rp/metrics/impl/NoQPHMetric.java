/**
 * 
 */
package org.aksw.iguana.rp.metrics.impl;

import java.util.Properties;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.metrics.AbstractMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * The Number Of Queries Per Hour Metric 
 * 
 * @author f.conrads
 *
 */
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

		Integer time = (Integer) p.get(COMMON.RECEIVE_DATA_TIME);
		Integer success = (Boolean) p.get(COMMON.RECEIVE_DATA_SUCCESS)?1:0;	
		
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
 
		for(Properties key : dataContainer.keySet()){
			Integer totalTime = (Integer) dataContainer.get(key).get(TOTAL_TIME);
			Integer success = (Integer) dataContainer.get(key).get(TOTAL_SUCCESS);
			Double noOfQueriesPerHour = hourInMS*success*1.0/totalTime;
			Properties results = new Properties();
			results.put("noOfQueriesPerHour", noOfQueriesPerHour);
			sendTriples(results, key);
		}
		
		
	}
		


}
