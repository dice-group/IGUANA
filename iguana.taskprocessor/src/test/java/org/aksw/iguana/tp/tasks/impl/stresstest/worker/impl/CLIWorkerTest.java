package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.aksw.iguana.tp.tasks.impl.stresstest.worker.Worker;
import org.junit.Test;

public class CLIWorkerTest {

	private String curl = "curl -X GET -q \"http://dbpedia.org/sparql?format=text%2Fcsv&query=$ENCODEDQUERY$\"";
	private String inputScript = "src/test/resources/inputTest.sh";
	
	/**
	 * Tests a normal successful execution
	 * @throws IOException
	 */
	@Test
	public void testTime() throws IOException {
		Worker worker = new CLIWorker(new String[] {"1", "1", null, curl,null,null, null,  "","0","0"});
		assertTrue((double)worker.getTimeForQueryMs("select * {?s ?p ?o} LIMIT 10", "10")[0]>0);
		
	}
	
	@Test
	public void testInput() throws IOException {
		Worker worker = new CLIInputWorker(new String[] {"1", "1", null, inputScript, null,null, null,  "","0","0", "test", "finished"});
		assertTrue((double)worker.getTimeForQueryMs("select * {?s ?p ?o} LIMIT 10", "10")[0]>0);
		
	}
}
