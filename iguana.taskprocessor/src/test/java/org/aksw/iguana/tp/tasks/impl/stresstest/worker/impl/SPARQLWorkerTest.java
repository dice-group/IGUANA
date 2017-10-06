package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.Worker;
import org.junit.Test;


/**
 * This will test if the {@link SPARQLWorker} works like expected
 * 
 * @author f.conrads
 *
 */
public class SPARQLWorkerTest {

	
	/**
	 * Tests a normal successful execution
	 * @throws IOException
	 */
	@Test
	public void testTime() throws IOException {
		Worker worker = new SPARQLWorker("1", 1, null, "http://dbpedia.org/sparql", null,  "",0,0);
		assertTrue(worker.getTimeForQueryMs("select * {?s ?p ?o}", "1")>0);
		
	}
	
	/**
	 * Tests if a timeOut occurs if it should
	 * FIXME running it manually it works. mvn build cannot proceed it somehow!
	 * @throws IOException
	 */
//	@Test
	public void testTimeout() throws IOException {
		Worker worker = new SPARQLWorker("1", 1, null, "http://dbpedia.org/sparql", 1l,  "",0,0);
		assertEquals(-1L, worker.getTimeForQueryMs("select * {?s ?p ?o}", "1"));
		
	}
	
	/**
	 * Tests the getNextQuery method
	 * @throws IOException
	 */
	@Test 
	public void testGetNextQuery() throws IOException {
		Worker worker = new SPARQLWorker("1", 1,  null, "http://dbpedia.org/sparql", 5l,  
				"",0,0);
		((AbstractWorker) worker).setQueriesList(new File[] {new File("src/test/resources/worker/sparql.sparql") });
		StringBuilder query = new StringBuilder();
		StringBuilder queryID = new StringBuilder();
		worker.getNextQuery(query, queryID);
		assertEquals("select bla", query.toString());
		assertEquals("sparql.sparql", queryID.toString());
	}

}
