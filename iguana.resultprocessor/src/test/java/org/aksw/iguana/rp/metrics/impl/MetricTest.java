/**
 * 
 */
package org.aksw.iguana.rp.metrics.impl;

import java.util.Properties;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.data.Triple;
import org.aksw.iguana.rp.metrics.Metric;
import org.aksw.iguana.rp.storage.StorageManager;
import org.aksw.iguana.rp.utils.EqualityStorage;
import org.junit.Test;

/**
 * This will do a small test with every implemented Metric
 * 
 * @author f.conrads
 *
 */
public class MetricTest {

	@Test
	public void test(){
		Properties extra = new Properties();
		extra.setProperty("a", "b");
		Triple meta = new Triple("1/1/1/"+extra.hashCode(), "a", "b");
		//
		// NoQPHMetric
		//
		Metric m = new NoQPHMetric();
		Triple[][] golden = new Triple[1][];
		Triple[] triple = new Triple[1];
		Double d = 4104.903078677;
		triple[0] = new Triple("1/1/1", "noOfQueriesPerHour", d);
		golden[0] = triple;
		test(m, golden, new Properties());
		
		m = new NoQPHMetric();
		triple = new Triple[2];
		triple[1] = new Triple("1/1/1/"+extra.hashCode(), "noOfQueriesPerHour", d);
		triple[0] = meta;
		golden[0] = triple;
		test(m, golden, extra);
		
		//
		// QMPHMetric
		//
		m = new QMPHMetric();
		triple = new Triple[1];
		d = 2052.451539338;
		triple[0] = new Triple("1/1/1", "queryMixes", d);
		golden[0] = triple;
		test(m, golden, new Properties());
		
		m = new QMPHMetric();
		triple = new Triple[2];
		d = 2052.451539338;
		triple[0] = meta;
		triple[1] = new Triple("1/1/1/"+extra.hashCode(), "queryMixes", d);
		golden[0] = triple;
		test(m, golden, extra);

		//
		// QPSMetric
		//
		m = new QPSMetric();
		triple = new Triple[6];
		triple[0] = new Triple("1/1/1", "qps#query", "1/1/1/1");
		triple[4] = new Triple("1/1/1/1", "queryID", 1);
		triple[3] = new Triple("1/1/1/1", "succeded", 1);
		triple[2] = new Triple("1/1/1/1", "failed", 1);
		triple[5] = new Triple("1/1/1/1", "totalTime", 877);
		triple[1] = new Triple("1/1/1/1", "queriesPerSecond", 1.140250855);
		golden[0] = triple;
		test(m, golden, new Properties());
		
		m = new QPSMetric();
		triple = new Triple[7];
		triple[0] = meta;
		triple[1] = new Triple("1/1/1/"+extra.hashCode(), "qps#query", "1/1/1/"+extra.hashCode()+"/1");
		triple[5] = new Triple("1/1/1/"+extra.hashCode()+"/1", "queryID", 1);
		triple[4] = new Triple("1/1/1/"+extra.hashCode()+"/1", "succeded", 1);
		triple[3] = new Triple("1/1/1/"+extra.hashCode()+"/1", "failed", 1);
		triple[6] = new Triple("1/1/1/"+extra.hashCode()+"/1", "totalTime", 877);
		triple[2] = new Triple("1/1/1/"+extra.hashCode()+"/1", "queriesPerSecond", 1.140250855);
		golden[0] = triple;
		test(m, golden, extra);
		
		//
		// EachQueryMetric
		//
		m = new EachQueryMetric();
		golden = new Triple[2][];
		triple = new Triple[5];
		triple[0] = new Triple("1/1/1/1", "EQE", "1/1/1/1/1");
		triple[4] = new Triple("1/1/1/1/1", "run", 1);
		triple[1] = new Triple("1/1/1/1/1", "time", 777);
		triple[2] = new Triple("1/1/1/1/1", "success", true);
		triple[3] = new Triple("1/1/1/1/1", "queryID", 1);
		golden[0] = triple;
		Triple[] triple2 = new Triple[5];
		triple2[0] = new Triple("1/1/1/1", "EQE", "1/1/1/1/2");
		triple2[4] = new Triple("1/1/1/1/2", "run", 2);
		triple2[1] = new Triple("1/1/1/1/2", "time", 100);
		triple2[2] = new Triple("1/1/1/1/2", "success", false);
		triple2[3] = new Triple("1/1/1/1/2", "queryID", 1);
		golden[1] = triple2;
		test(m, golden, new Properties());
		
		meta = new Triple("1/1/1/"+extra.hashCode()+"/1", "a", "b");
		m = new EachQueryMetric();
		golden = new Triple[2][];
		triple = new Triple[6];
		triple[0] = meta;
		triple[1] = new Triple("1/1/1/"+extra.hashCode()+"/1", "EQE", "1/1/1/"+extra.hashCode()+"/1/1");
		triple[5] = new Triple("1/1/1/"+extra.hashCode()+"/1/1", "run", 1);
		triple[2] = new Triple("1/1/1/"+extra.hashCode()+"/1/1", "time", 777);
		triple[3] = new Triple("1/1/1/"+extra.hashCode()+"/1/1", "success", true);
		triple[4] = new Triple("1/1/1/"+extra.hashCode()+"/1/1", "queryID", 1);
		golden[0] = triple;
		triple2 = new Triple[6];
		triple2[0] = meta;
		triple2[1] = new Triple("1/1/1/"+extra.hashCode()+"/1", "EQE", "1/1/1/"+extra.hashCode()+"/1/2");
		triple2[5] = new Triple("1/1/1/"+extra.hashCode()+"/1/2", "run", 2);
		triple2[2] = new Triple("1/1/1/"+extra.hashCode()+"/1/2", "time", 100);
		triple2[3] = new Triple("1/1/1/"+extra.hashCode()+"/1/2", "success", false);
		triple2[4] = new Triple("1/1/1/"+extra.hashCode()+"/1/2", "queryID", 1);
		golden[1] = triple2;
		test(m, golden, extra);
		
		
	
	}
	
	public void test(Metric metric, Triple[][] golden, Properties extraMeta){

		StorageManager smanager = new StorageManager();
		smanager.addStorage(new EqualityStorage(golden));
		metric.setStorageManager(smanager);
		
		Properties p = new Properties();
		p.put(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.setProperty(COMMON.EXPERIMENT_ID_KEY, "1/1");
	    p.setProperty(COMMON.CONNECTION_ID_KEY, "virtuoso");
	    p.setProperty(COMMON.SUITE_ID_KEY, "1");
	    p.setProperty(COMMON.DATASET_ID_KEY, "dbpedia");
	    p.put(COMMON.RECEIVE_DATA_START_KEY, "true");
	    p.put(COMMON.EXTRA_META_KEY, new Properties());
	    p.put(COMMON.NO_OF_QUERIES, 2);
	    
	    metric.setMetaData(p);
	    
	    p = new Properties();
		p.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.put(COMMON.RECEIVE_DATA_SUCCESS, true);
	    p.put(COMMON.RECEIVE_DATA_TIME, 777);
	    p.put(COMMON.QUERY_ID_KEY, "1");
	    p.put(COMMON.EXTRA_META_KEY, extraMeta);
		
		metric.receiveData(p);
		
		p = new Properties();
		p.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.put(COMMON.RECEIVE_DATA_SUCCESS, false);
	    p.put(COMMON.RECEIVE_DATA_TIME, 100);
	    p.put(COMMON.QUERY_ID_KEY, "1");
	    p.put(COMMON.EXTRA_META_KEY, extraMeta);
		
		metric.receiveData(p);
		metric.close();
	}
}
