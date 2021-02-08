package org.aksw.iguana.rp.metrics.impl;

import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.metrics.AbstractMetric;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Properties;

/**
 * Counts the number of all successfully executed queries
 */
@Shorthand("NoQ")
public class NoQMetric extends AbstractMetric {

    protected static final Object TOTAL_TIME = "totalTime";
    protected static final Object TOTAL_SUCCESS = "totalSuccess";


    protected static Logger LOGGER = LoggerFactory.getLogger(NoQPHMetric.class);


    protected long hourInMS = 3600000;


    public NoQMetric(){
        super("Number Of Queries", "NoQ", "Will calculate the number of queries which could be executed successfully.");
    }

    protected NoQMetric(String name, String shortName, String description){
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
        long sum = 0;
        for(Properties key : dataContainer.keySet()){
            Integer success = (Integer) dataContainer.get(key).get(TOTAL_SUCCESS);
            sum+=success;
            Resource subject = getSubject(key);
            m.add(getConnectingStatement(subject));
            m.add(subject, property, ResourceFactory.createTypedLiteral(BigInteger.valueOf(success)));
        }
        m.add(getTaskResource(), property, ResourceFactory.createTypedLiteral(BigInteger.valueOf(sum)));
        sendData(m);
    }



}
