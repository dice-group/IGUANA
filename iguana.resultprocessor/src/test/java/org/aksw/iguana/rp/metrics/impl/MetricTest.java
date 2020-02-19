/**
 * 
 */
package org.aksw.iguana.rp.metrics.impl;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.data.Triple;
import org.aksw.iguana.rp.metrics.Metric;
import org.aksw.iguana.rp.storage.StorageManager;
import org.aksw.iguana.rp.utils.EqualityStorage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

/**
 * This will do a small test with every implemented Metric
 * 
 * @author f.conrads
 *
 */
@RunWith(Parameterized.class)
public class MetricTest {

	private Properties extra;
	//private Triple meta;
	private Metric m;
	private Triple[][] triples;
	private boolean changeMeta;

	/**
	 * @return Configurations to test
	 */
	@Parameters
	public static Collection<Object[]> data() {
		List<Object[]> testConfigs = new ArrayList<Object[]>();

		testConfigs.add(new Object[] { new NoQPHMetric(), new Triple[][]{{new Triple("1/1/1", "noOfQueriesPerHour", 4104.903078677) }}, false});
//		testConfigs.add(new Object[] { new QMPHMetric(), new Triple[][]{{new Triple("1/1/1", "queryMixes", 2052.451539338) }}, false});
		testConfigs.add(new Object[] { new QPSMetric(), new Triple[][]{{new Triple("1/1/1", "qps#query", "1/1/1/1"),
			new Triple("1/1/1/1", "queriesPerSecond", 1.140250855),
			new Triple("1/1/1/1", "failed", 1),
				new Triple("1/1/1/1", "succeeded", 1),
			new Triple("1/1/1/1", "queryID", 1),
			new Triple("1/1/1/1", "totalTime", 877)}}, false});
		testConfigs.add(new Object[] { new EachQueryMetric(), new Triple[][]{{new Triple("1/1/1/1", "EQE", "1/1/1/1/1"), 
			new Triple("1/1/1/1/1", "time", 777),
			new Triple("1/1/1/1/1", "success", true),
			new Triple("1/1/1/1/1", "queryID", 1),
			new Triple("1/1/1/1/1", "run", 1)},
			{
			new Triple("1/1/1/1", "EQE", "1/1/1/1/2"),
			new Triple("1/1/1/1/2", "time", 100),
			new Triple("1/1/1/1/2", "success", false),
			new Triple("1/1/1/1/2", "queryID", 1),
			new Triple("1/1/1/1/2", "run", 2)
			}}, true});
			

		return testConfigs;
	}

	
	
	public MetricTest(Metric m, Triple[][] triples, boolean changeMeta) {
		
		extra = new Properties();
		extra.setProperty("a", "b");
		//meta = new Triple("1/1/1/"+extra.hashCode(), "a", "b");
		this.m = m;
		this.triples = triples;
		this.changeMeta = changeMeta;
	}
	
	@Test
	public void test() throws InstantiationException, IllegalAccessException{
		Triple meta = new Triple("1/1/1/"+extra.hashCode(), "a", "b");
		Triple[][] golden = new Triple[triples.length][];
		
		for(int i=0;i<triples.length;i++) {
			golden[i] = triples[i];
		}
		assertTrue(test(m, golden, new Properties()));
		
		m = m.getClass().newInstance();
		golden = new Triple[triples.length][];
		if(this.changeMeta) {
			meta = new Triple("1/1/1/"+extra.hashCode()+"/1", "a", "b");
		}
		for(int i=0;i<triples.length;i++) {
			Triple[] triplesWithMeta = new Triple[triples[i].length+1];
			triplesWithMeta[0] = meta;
			Triple[] t = triples[i];
			for(int j=0;j<t.length;j++) {
				String subject = t[j].getSubject();
				subject = subject.replaceFirst("1/1/1", "1/1/1/"+extra.hashCode());
				t[j].setSubject(subject);
				triplesWithMeta[j+1]= t[j];
			}
			golden[i] = triplesWithMeta;
		}
		assertTrue(test(m, golden, extra));
	
	}
	
	public boolean test(Metric metric, Triple[][] golden, Properties extraMeta){

		StorageManager smanager = new StorageManager();
		EqualityStorage storage = new EqualityStorage(golden);
		smanager.addStorage(storage);
		metric.setStorageManager(smanager);
	    metric.setMetaData(createMetaData());
		metric.receiveData(createData(777, true, extraMeta));
		metric.receiveData(createData(100, false, extraMeta));
		
		metric.close();
		return storage.isLastCheck();

	}
	
	private Properties createData(long time, boolean success, Properties extraMeta) {
		Properties p = new Properties();
		p.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.put(COMMON.RECEIVE_DATA_SUCCESS, success);
	    p.put(COMMON.RECEIVE_DATA_TIME, time);
	    p.put(COMMON.QUERY_ID_KEY, "1");
	    p.put(COMMON.EXTRA_META_KEY, extraMeta);
	    return p;
	}
	
	private Properties createMetaData() {
		Properties p = new Properties();
		p.put(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.setProperty(COMMON.EXPERIMENT_ID_KEY, "1/1");
	    p.setProperty(COMMON.CONNECTION_ID_KEY, "virtuoso");
	    p.setProperty(COMMON.SUITE_ID_KEY, "1");
	    p.setProperty(COMMON.DATASET_ID_KEY, "dbpedia");
	    p.put(COMMON.RECEIVE_DATA_START_KEY, "true");
	    p.put(COMMON.EXTRA_META_KEY, new Properties());
	    p.put(COMMON.NO_OF_QUERIES, 2);
	    return p;
	}
}
