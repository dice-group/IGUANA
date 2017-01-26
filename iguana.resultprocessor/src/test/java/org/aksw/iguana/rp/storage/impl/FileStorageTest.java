/**
 * 
 */
package org.aksw.iguana.rp.storage.impl;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.config.CONSTANTS;
import org.aksw.iguana.rp.data.Triple;
import org.aksw.iguana.rp.storage.Storage;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

/**
 * 
 * Will test The FileStorage directory structure and the content of the csv files
 * 
 * @author f.conrads
 *
 */
public class FileStorageTest {

	@Test
	public void test() throws IOException{
		Storage storage = new FileStorage();
		
	    FileUtils.deleteDirectory(new File("results_storage"));

		Properties extraMeta = new Properties();
		extraMeta.setProperty("a", "b");
		
		Properties p = new Properties();
		p.put(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.setProperty(COMMON.EXPERIMENT_ID_KEY, "1/1");
	    p.setProperty(COMMON.CONNECTION_ID_KEY, "virtuoso");
	    p.setProperty(COMMON.SUITE_ID_KEY, "1");
	    p.setProperty(COMMON.DATASET_ID_KEY, "dbpedia");
	    p.put(COMMON.RECEIVE_DATA_START_KEY, "true");
	    p.put(COMMON.EXTRA_META_KEY, new Properties());
	    p.put(COMMON.NO_OF_QUERIES, 2);
	    
	    storage.addMetaData(p);
	    File dir = new File("results_storage"+File.separator+"SuiteID 1"+File.separator+
	    		"ExperimentID 1--1"+File.separator+
	    		"Extra_Meta_Hash 0");
	    File dataset = new File("results_storage"+File.separator+"SuiteID 1"+File.separator+
	    		"ExperimentID 1--1"+File.separator+"dbpedia");
	    File extraProps = new File(dir.getAbsolutePath()+File.separator+"extraProperties");
	    assertTrue(dataset.exists());
	    assertTrue(dir.isDirectory());
	    assertTrue(FileUtils.readLines(extraProps, Charset.defaultCharset()).isEmpty());
	    assertTrue(FileUtils.readLines(dataset, Charset.defaultCharset()).get(0).trim().equals("Dataset: dbpedia"));
	    
	    Triple[] t = new Triple[1];
	    t[0] = new Triple("a", "b", "c");
	    // send data wo extra
	    p = new Properties();
		p.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.put(COMMON.METRICS_PROPERTIES_KEY, "testMetric");
	    p.put(CONSTANTS.LENGTH_EXTRA_META_KEY, 0);
	    p.put(COMMON.EXTRA_META_KEY, new Properties());
	    storage.addData(p, t);
	    File metric = new File(dir.getAbsolutePath()+File.separator+"testMetric"+File.separator+"testMetric.csv");
	    assertTrue(metric.exists());
	    List<String> lines = FileUtils.readLines(metric, Charset.defaultCharset());
	    assertTrue(lines.get(0).trim().equals("connectionID\tb"));
	    assertTrue(lines.get(1).trim().equals("virtuoso\tc"));
	    
	    //send data w extra
	    p.put(CONSTANTS.LENGTH_EXTRA_META_KEY, 1);
	    t = new Triple[2];
	    t[0] = new Triple("a", "a", "b");
	    t[1] = new Triple("a", "b", "c");
	    storage.addData(p, t);
	    metric = new File(dir.getAbsolutePath()+File.separator+"testMetric"+File.separator+"a-b.csv");
	    assertTrue(metric.exists());
	    lines = FileUtils.readLines(metric, Charset.defaultCharset());
	    assertTrue(lines.get(0).trim().equals("connectionID\tb"));
	    assertTrue(lines.get(1).trim().equals("virtuoso\tc"));
	   
	    FileUtils.deleteDirectory(new File("results_storage"));
	    
	    p = new Properties();
		p.put(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.setProperty(COMMON.EXPERIMENT_ID_KEY, "1/1");
	    p.setProperty(COMMON.CONNECTION_ID_KEY, "virtuoso");
	    p.setProperty(COMMON.SUITE_ID_KEY, "1");
	    p.setProperty(COMMON.DATASET_ID_KEY, "dbpedia");
	    p.put(COMMON.RECEIVE_DATA_START_KEY, "true");
	    p.put(COMMON.EXTRA_META_KEY, extraMeta);
	    p.put(COMMON.NO_OF_QUERIES, 2);
	    storage.addMetaData(p);
	    dir = new File("results_storage"+File.separator+"SuiteID 1"+File.separator+
	    		"ExperimentID 1--1"+File.separator+
	    		"Extra_Meta_Hash "+extraMeta.hashCode());
	    dataset = new File("results_storage"+File.separator+"SuiteID 1"+File.separator+
	    		"ExperimentID 1--1"+File.separator+"dbpedia");
	    extraProps = new File(dir.getAbsolutePath()+File.separator+"extraProperties");
	    assertTrue(dataset.exists());
	    assertTrue(dir.isDirectory());
	    assertTrue(FileUtils.readLines(extraProps, Charset.defaultCharset()).get(0).trim().equals("a:\tb"));
	    assertTrue(FileUtils.readLines(dataset, Charset.defaultCharset()).get(0).trim().equals("Dataset: dbpedia"));
	    
	    t = new Triple[1];
	    t[0] = new Triple("a", "b", "c");
	    // send data wo extra
	    p = new Properties();
		p.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.put(COMMON.METRICS_PROPERTIES_KEY, "testMetric");
	    p.put(CONSTANTS.LENGTH_EXTRA_META_KEY, 0);
	    p.put(COMMON.EXTRA_META_KEY, new Properties());
	    storage.addData(p, t);
	    metric = new File(dir.getAbsolutePath()+File.separator+"testMetric"+File.separator+"testMetric.csv");
	    assertTrue(metric.exists());
	    lines = FileUtils.readLines(metric, Charset.defaultCharset());
	    assertTrue(lines.get(0).trim().equals("connectionID\tb"));
	    assertTrue(lines.get(1).trim().equals("virtuoso\tc"));
	    
	    //send data w extra
	    p.put(CONSTANTS.LENGTH_EXTRA_META_KEY, 1);
	    t = new Triple[2];
	    t[0] = new Triple("a", "a", "b");
	    t[1] = new Triple("a", "b", "c");
	    storage.addData(p, t);
	    metric = new File(dir.getAbsolutePath()+File.separator+"testMetric"+File.separator+"a-b.csv");
	    assertTrue(metric.exists());
	    lines = FileUtils.readLines(metric, Charset.defaultCharset());
	    assertTrue(lines.get(0).trim().equals("connectionID\tb"));
	    assertTrue(lines.get(1).trim().equals("virtuoso\tc"));
	    
	    FileUtils.deleteDirectory(new File("results_storage"));
	}

	

}
