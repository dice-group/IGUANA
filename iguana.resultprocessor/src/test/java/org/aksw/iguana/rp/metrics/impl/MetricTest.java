/**
 * 
 */
package org.aksw.iguana.rp.metrics.impl;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.metrics.Metric;
import org.aksw.iguana.rp.storage.StorageManager;
import org.aksw.iguana.rp.utils.EqualityStorage;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * This will do a small test with every implemented Metric
 * 
 * @author f.conrads
 *
 */
@RunWith(Parameterized.class)
public class MetricTest {

	private final Model goldenModel;
	private Properties extra = new Properties();
	private Metric m;
	private boolean sendPenalty;

	/**
	 * @return Configurations to test
	 */
	@Parameters
	public static Collection<Object[]> data() {
		List<Object[]> testConfigs = new ArrayList<Object[]>();

		testConfigs.add(new Object[] { new NoQPHMetric(),"src/test/resources/nt/noqphtest.nt", false});
		testConfigs.add(new Object[] { new QPSMetric(), "src/test/resources/nt/qpstest.nt", false});
		//check if penalty will be used if send.
		testConfigs.add(new Object[] { new QPSMetric(), "src/test/resources/nt/qpspenaltytest.nt", true});
		testConfigs.add(new Object[] { new QPSMetric(1000), "src/test/resources/nt/qpspenaltytest.nt", false});
		//Test if 2000 will be used instead of provided 1000
		testConfigs.add(new Object[] { new QPSMetric(2000), "src/test/resources/nt/qpspenaltytest2.nt", true});
		testConfigs.add(new Object[] { new AvgQPSMetric(), "src/test/resources/nt/avgqpstest.nt", false});
		testConfigs.add(new Object[] { new NoQMetric(), "src/test/resources/nt/noqtest.nt", false});
		testConfigs.add(new Object[] { new QMPHMetric(), "src/test/resources/nt/qmphtest.nt", false});
		testConfigs.add(new Object[] { new EachQueryMetric(), "src/test/resources/nt/eqtest.nt", false});
		testConfigs.add(new Object[] { new F1MeasureMetric(), "src/test/resources/nt/f1test.nt", false});

		return testConfigs;
	}

	
	
	public MetricTest(Metric m, String golden, boolean sendPenalty) throws FileNotFoundException {

		//meta = new Triple("1/1/1/"+extra.hashCode(), "a", "b");
		this.m = m;
		this.goldenModel = ModelFactory.createDefaultModel();
		this.goldenModel.read(new FileReader(golden), null, "N-TRIPLE");
		this.sendPenalty=sendPenalty;
	}

	@Test
	public void modelTest(){
		Model[] data = test(m, goldenModel);
		//assert equals all triples in one are the same as the other
		assertEquals(data[0].size(), data[1].size());
		data[0].remove(data[1]);
		//if size was the same, and after EXPECTED <- EXPECTED/ACTUAL is either 0 if EXPECTED=ACTUAL or not zero, and the size of expected is bigger than 0
		assertEquals(0, data[0].size());
	}


	public Model[] test(Metric metric, Model golden){

		StorageManager smanager = new StorageManager();
		EqualityStorage storage = new EqualityStorage(golden);
		smanager.addStorage(storage);
		metric.setStorageManager(smanager);
	    metric.setMetaData(createMetaData());
	    Properties extraMeta = new Properties();
	    extraMeta.put(COMMON.WORKER_ID, "0");
		extraMeta.put(COMMON.NO_OF_QUERIES, 2);
		metric.receiveData(createData(200, "sparql1", "1123",120, 1, extraMeta));
		metric.receiveData(createData(250, "sparql2", "1125",100,1, extraMeta));
		extraMeta = new Properties();
		extraMeta.put(COMMON.WORKER_ID, "1");
		extraMeta.put(COMMON.NO_OF_QUERIES, 2);
		metric.receiveData(createData(150, "sparql1", "1123", null, 1, extraMeta));
		metric.receiveData(createData(100, "sparql2", "1125",null,-2L, extraMeta));
		
		metric.close();
		return new Model[]{storage.getExpectedModel(), storage.getActualModel()};

	}
	
	private Properties createData(double time, String queryID, String queryHash, Integer resultSize, long success, Properties extraMeta) {
		Properties p = new Properties();
		p.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.put(COMMON.RECEIVE_DATA_SUCCESS, success);
	    p.put(COMMON.RECEIVE_DATA_TIME, time);
	    p.put(COMMON.FULL_QUERY_ID_KEY, queryID);
		p.put(COMMON.QUERY_HASH, queryHash);
		p.put(COMMON.QUERY_STRING, "SELECT * {?s ?p ?o}");
		//tp=time/5, fp=time/10, fn=8
		p.put(COMMON.DOUBLE_RAW_RESULTS, new double[]{time/5.0, time/10.0, 8});
		if(this.sendPenalty)
			p.put(COMMON.PENALTY, 1000);
		if(resultSize!=null)
			p.put(COMMON.RECEIVE_DATA_SIZE, resultSize);
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
	    p.put(COMMON.EXTRA_META_KEY, extra);
	    p.put(COMMON.NO_OF_QUERIES, 2);
	    return p;
	}
}
