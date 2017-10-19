/**
 * 
 */
package org.aksw.iguana.rp.storage.impl;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.data.Triple;
import org.aksw.iguana.rp.storage.Storage;
import org.junit.Test;

/**
 * 
 * This will test the NTFileStorage in short.
 * 
 * 
 * @author f.conrads
 *
 */
public class NTFileStorageTest {

	
	@Test
	public void dataTest() throws IOException{
		Storage store = new NTFileStorage("results_test2.nt");
		
		Properties extraMeta = new Properties();
		extraMeta.setProperty("a", "b");
	    new File("results_test2.nt").delete();

		Properties p = new Properties();
		p.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.put(COMMON.METRICS_PROPERTIES_KEY, "testMetric");
	    p.put(COMMON.EXTRA_META_KEY, new Properties());
	    
	    Triple[] t = new Triple[1];
	    t[0] = new Triple("a", "b", "c");
	    store.addData(p, t);
	    store.commit();
	    assertEqual("results_test2.nt","src/test/resources/nt/results_test1.nt");
	    new File("results_test2.nt").delete();

	}
	
	@Test
	public void metaTest() throws IOException{
		Storage store = new NTFileStorage("results_test.nt");
	    new File("results_test.nt").delete();

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
	    
	    store.addMetaData(p);
	    assertEqual("results_test.nt", "src/test/resources/nt/nt_results_woMeta.nt");
	    new File("results_test.nt").delete();

	    
	    p.put(COMMON.EXTRA_META_KEY, extraMeta);
	    store.addMetaData(p);
	    assertEqual("results_test.nt", "src/test/resources/nt/nt_results_wMeta.nt");
	    
	    new File("results_test.nt").delete();
	    

	}

	public void assertEqual(String file1, String file2) throws IOException{
		String actual="";
	    try(BufferedReader reader = new BufferedReader(new FileReader(file1))){
	    	String line = "";
	    	while((line=reader.readLine())!=null){
	    		if(line.trim().isEmpty()){
	    			continue;
	    		}
	    		actual+=line+"\n";
	    	}
	    	
	    }
	    String expect="";
	    try(BufferedReader reader = new BufferedReader(new FileReader(file2))){
	    	String line = "";
	    	while((line=reader.readLine())!=null){
	    		if(line.trim().isEmpty()){
	    			continue;
	    		}
	    		expect+=line+"\n";
	    	}
	    	
	    }
	    
	    assertEquals(expect.trim(), actual.trim());
	}
}
