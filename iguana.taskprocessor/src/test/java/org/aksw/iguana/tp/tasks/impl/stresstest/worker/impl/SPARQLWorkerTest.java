package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;


import org.aksw.iguana.tp.tasks.impl.stresstest.worker.Worker;
import org.junit.Test;


/**
 * This will test if the {@link SPARQLWorker} works like expected
 * 
 * @author f.conrads
 *
 */
public class SPARQLWorkerTest {

	

	@Test
	public void testTime() throws IOException {
		Worker worker = new SPARQLWorker("http://dbpedia.org/sparql", null, "1", 1, "SPARQL", new File[] {new File("a")},0,0);
		assertTrue(worker.getTimeForQueryMs("select * {?s ?p ?o}", "1")>0);
		
	}
	
	@Test
	public void testTimeout() throws IOException {
		Worker worker = new SPARQLWorker("http://dbpedia.org/sparql", 5l, "1", 1, "SPARQL", new File[] {new File("a")},0,0);
		assertEquals(-1, worker.getTimeForQueryMs("select * {?s ?p ?o}", "1"));
		
	}
	
	@Test 
	public void testGetNextQuery() throws IOException {
		Worker worker = new SPARQLWorker("http://dbpedia.org/sparql", 5l, "1", 1, "SPARQL", 
				new File[] {new File("src/test/resources/worker/sparql.sparql")},0,0);
		StringBuilder query = new StringBuilder();
		StringBuilder queryID = new StringBuilder();
		worker.getNextQuery(query, queryID);
		assertEquals("select bla", query.toString());
		assertEquals("sparql.sparql", queryID.toString());
	}

}
