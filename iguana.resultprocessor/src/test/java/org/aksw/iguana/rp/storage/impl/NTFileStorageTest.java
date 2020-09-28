/**
 * 
 */
package org.aksw.iguana.rp.storage.impl;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.storage.Storage;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

	    new File("results_test2.nt").delete();

	    Model m = ModelFactory.createDefaultModel();
	    m.read(new FileReader("src/test/resources/nt/results_test1.nt"), null, "N-TRIPLE");

	    store.addData(m);
	    store.commit();
	    assertEqual("results_test2.nt","src/test/resources/nt/results_test1.nt", true);
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
	    p.put(COMMON.EXPERIMENT_TASK_CLASS_ID_KEY, "ClassName");
	    p.put(COMMON.EXTRA_META_KEY, new Properties());
	    p.put(COMMON.NO_OF_QUERIES, 2);
	    
	    store.addMetaData(p);
	    store.commit();
	    assertEqual("results_test.nt", "src/test/resources/nt/nt_results_woMeta.nt", false);
	    new File("results_test.nt").delete();
		store = new NTFileStorage("results_test2.nt");
	    
	    p.put(COMMON.EXTRA_META_KEY, extraMeta);
	    store.addMetaData(p);
	    store.commit();
	    assertEqual("results_test2.nt", "src/test/resources/nt/nt_results_wMeta.nt", false);
	    
	    new File("results_test.nt2").delete();
	    

	}

	/**
	 * Checks if two ntriple files are equal by loading them into a model and check if they have the same size
	 * and by removing the actual model from the expected, if the new size after removal equals 0 they are the same
	 *
	 * @param actualFile
	 * @param expectedFile
	 * @throws IOException
	 */
	public void assertEqual(String actualFile, String expectedFile, boolean ignoreDate) throws IOException{
		Model expected = ModelFactory.createDefaultModel();
		expected.read(new FileReader(expectedFile), null, "N-TRIPLE");
		Model actual = ModelFactory.createDefaultModel();
		actual.read(new FileReader(actualFile), null, "N-TRIPLE");
		assertEquals(expected.size(), actual.size());
		expected.remove(actual);
		if(!ignoreDate){
			//Remove startDate as they are different, just check if actual contains a start date
			Property startDate =ResourceFactory.createProperty(RDFS.getURI()+"startDate");
			assertTrue(actual.contains(null, startDate, (RDFNode)null));
			List<Statement> stmts = expected.listStatements(null, startDate, (RDFNode)null).toList();
			assertEquals(1, stmts.size());
			expected.remove(stmts);
		}

		assertEquals(0, expected.size());
	}
}
