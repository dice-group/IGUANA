package org.aksw.iguana.rp.metrics.impl;

import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.apache.jena.rdf.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Calculates the average queries per second
 */
@Shorthand("AvgQPS")
public class AvgQPSMetric extends QPSMetric {
    public AvgQPSMetric() {
        super(
                "Average Queries Per Second",
                "AvgQPS",
                "Will calculate the overall average queries Per second. Further on it will save the totaltime of each query, the failure and the success");
    }

    public AvgQPSMetric(Integer penalty) {
        super(
                "Average Queries Per Second",
                "AvgQPS",
                "Will calculate the overall average queries Per second. Further on it will save the totaltime of each query, the failure and the success");
        this.penalty=penalty;
    }



    @Override
    public void close() {
        super.close();
    }

    @Override
    protected void qpsClose(){
        Model m = ModelFactory.createDefaultModel();
        Map<Object, Number[]> map = new HashMap<Object, Number[]>();
        Property property = getMetricProperty();
        Property penalziedProp = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"penalized"+shortName);
        for(Properties key : dataContainer.keySet()){
            Properties value = dataContainer.get(key);
            Double avgQps=0.0;
            Double penalizedAvgQps=0.0;
            for(Object queryID : value.keySet()){
                Object[] resArr = (Object[]) value.get(queryID);
                Double qps = (long)resArr[1]*1.0/((double)resArr[0]/1000.0);
                Double penalizedQPS = (long)resArr[1]*1.0/((double)resArr[7]/1000.0);
                map.putIfAbsent(queryID, new Number[]{Double.valueOf(0), Long.valueOf(0), Double.valueOf(0)});

                Number[] current =map.get(queryID);
                Long succ = (long)resArr[1]+(Long)current[1];
                Double time = (double)resArr[0]+(Double)current[0];
                Double penTime = (double)resArr[7]+(Double)current[2];
                map.put(queryID, new Number[]{time, succ, penTime});
                avgQps+=qps;
                penalizedAvgQps+=penalizedQPS;
            }
            avgQps = avgQps/value.size();
            penalizedAvgQps = penalizedAvgQps/value.size();
            Resource subject = getSubject(key);
            m.add(getConnectingStatement(subject));
            m.add(subject, property, ResourceFactory.createTypedLiteral(avgQps));
            m.add(subject, penalziedProp, ResourceFactory.createTypedLiteral(penalizedAvgQps));

        }
        Double avgQps=0.0;
        Double penalizedAvgQps=0.0;
        for(Object queryID : map.keySet()) {
            Double qps = (Long)map.get(queryID)[1]*1.0/((Double)map.get(queryID)[0]/1000.0);
            Double penalizedQPS = (long)map.get(queryID)[1]*1.0/((double)map.get(queryID)[2]/1000.0);
            avgQps+=qps;
            penalizedAvgQps+=penalizedQPS;
        }
        avgQps = avgQps/map.size();
        penalizedAvgQps= penalizedAvgQps/map.size();
        m.add(getTaskResource(), property, ResourceFactory.createTypedLiteral(avgQps));
        m.add(getTaskResource(), penalziedProp, ResourceFactory.createTypedLiteral(penalizedAvgQps));
        this.sendData(m);
        this.storageManager.commit();
    }

}
