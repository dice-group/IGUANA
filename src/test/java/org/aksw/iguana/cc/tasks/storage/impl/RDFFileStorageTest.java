/**
 * 
 */
package org.aksw.iguana.cc.tasks.storage.impl;

import org.aksw.iguana.cc.tasks.stresstest.storage.impl.RDFFileStorage;
import org.aksw.iguana.cc.tasks.stresstest.storage.Storage;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 
 * This will test the RDFFileStorage in short.
 * 
 * @author l.conrads
 *
 */
public class RDFFileStorageTest {

	
	@Test
	public void dataTest() throws IOException{
		Storage store = new RDFFileStorage("results_test2.ttl");

	    new File("results_test2.ttl").delete();

	    Model m = ModelFactory.createDefaultModel();
	    m.read(new FileReader("src/test/resources/nt/results_test1.nt"), null, "N-TRIPLE");

	    store.storeResult(m);

	    assertEqual("results_test2.ttl","src/test/resources/nt/results_test1.nt", true);
	    new File("results_test2.ttl").delete();

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
		actual.read(new FileReader(actualFile), null, RDFLanguages.filenameToLang(actualFile).getName());
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
