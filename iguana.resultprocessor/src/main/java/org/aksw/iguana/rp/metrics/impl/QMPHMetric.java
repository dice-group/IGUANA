/**
 * 
 */
package org.aksw.iguana.rp.metrics.impl;

import java.util.Properties;

import org.aksw.iguana.commons.constants.COMMON;
import org.slf4j.LoggerFactory;

/**
 * 
 * The Query Mixes Per Hour Metric 
 * 
 * @author f.conrads
 * 
 */
public class QMPHMetric extends NoQPHMetric {
		
	public QMPHMetric(){
		super("Query Mixes Per Hour", "QMPH", "Will calculate the query mixes which could be executed successfully per Hour.");
		LOGGER = LoggerFactory.getLogger(QMPHMetric.class);
	}
	


	/* (non-Javadoc)
	 * @see org.aksw.iguana.rp.metrics.Metric#close()
	 */
	@Override
	public void close() {

		for(Properties key : dataContainer.keySet()){
			Double totalTime = (double) dataContainer.get(key).get(TOTAL_TIME);
			Integer success = (Integer) dataContainer.get(key).get(TOTAL_SUCCESS);
			
			double noOfQueriesPerHour = hourInMS*success*1.0/totalTime;
			
			Double noOfQueryMixes = (double) key.get(COMMON.NO_OF_QUERIES);
			Double qmph=noOfQueriesPerHour*1.0/noOfQueryMixes;
			
			Properties results = new Properties();
			results.put("queryMixes", qmph);
			sendTriples(results, key);
			this.storageManager.commit();
		}
		super.close();
	}

}
