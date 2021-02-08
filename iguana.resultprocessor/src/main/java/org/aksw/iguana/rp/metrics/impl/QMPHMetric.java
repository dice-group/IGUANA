/**
 * 
 */
package org.aksw.iguana.rp.metrics.impl;

import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.apache.jena.rdf.model.*;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Properties;

/**
 * 
 * The Query Mixes Per Hour Metric 
 * 
 * @author f.conrads
 * 
 */
@Shorthand("QMPH")
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
		callbackClose();
		super.close();
	}

	/**
	 * callback which will be called in close
	 */
	@Override
	protected void callbackClose(){
		Model m = ModelFactory.createDefaultModel();
		Property property = getMetricProperty();
		double sum = 0.0;
		for(Properties key : dataContainer.keySet()){
			double totalTime = (double) dataContainer.get(key).get(TOTAL_TIME);
			Integer success = (Integer) dataContainer.get(key).get(TOTAL_SUCCESS);

			double noOfQueriesPerHour = hourInMS*success*1.0/totalTime;

			int noOfQueryMixes = (int) key.get(COMMON.NO_OF_QUERIES);
			double qmph= noOfQueriesPerHour /noOfQueryMixes;

			sum+=qmph;
			Resource subject = getSubject(key);
			m.add(getConnectingStatement(subject));
			m.add(subject, property, ResourceFactory.createTypedLiteral(BigDecimal.valueOf(qmph)));
		}
		m.add(getTaskResource(), property, ResourceFactory.createTypedLiteral(BigDecimal.valueOf(sum)));
		sendData(m);
	}
}
