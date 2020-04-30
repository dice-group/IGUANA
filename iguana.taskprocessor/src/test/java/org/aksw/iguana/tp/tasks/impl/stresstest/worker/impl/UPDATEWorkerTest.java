package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.Worker;
import org.aksw.iguana.tp.utils.ServerMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.SocketConnection;

/**
 * This will test if the {@link SPARQLWorker} works like expected
 * 
 * @author f.conrads
 *
 */
// @RunWith(Parameterized.class)
public class UPDATEWorkerTest {

	private SocketConnection fastConnection;
	private InetSocketAddress address1;
	private String host = "http://localhost:8023";


	/**
	 * @throws IOException
	 */
	public UPDATEWorkerTest() throws IOException {
		ServerMock mock = new ServerMock();
		ContainerServer fastServer = new ContainerServer(mock);
		fastConnection = new SocketConnection(fastServer);
		address1 = new InetSocketAddress(8023);
	}

	/**
	 * Starts the Server Mock
	 * @throws IOException
	 */
	@Before
	public void startServer() throws IOException {
		fastConnection.connect(address1);
	}
	

	/**
	 * tests a normal successful execution
	 * @throws IOException
	 */
	@Test
	public void testTime() throws IOException {
		Worker worker = new UPDATEWorker(new String[] {"1", "1", null, host, null, null,  null,
				"", "0", "0", "NONE", "NONE"});
//		double time = worker.executeQuery("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
//				+ "INSERT DATA { <http://example/egbook3> dc:title  \"This is an example title\" }", "1").getExecutionTime();
//		assertTrue(time > 0);
		worker.executeQuery("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
				+ "INSERT DATA { <http://example/egbook3> dc:title  \"This is an example title\" }", "1");
	}

	/**
	 * Tests if the timeout works
	 * @throws IOException
	 */
	@Test
	public void testTimeout() throws IOException {
		Worker worker = new UPDATEWorker(new String[] {"1", "1", null, host, null, null,   "1", 
				"", "0", "0", "NONE","NONE"});
//		double time = (double)worker.executeQuery("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
//				+ "INSERT DATA { <http://example/egbook3> dc:title  \"This is an example title\" }", "1").getExecutionTime();
//		assertEquals(-1, time);
		worker.executeQuery("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
				+ "INSERT DATA { <http://example/egbook3> dc:title  \"This is an example title\" }", "1");
	}

	/**
	 * Tests the getNextQuery method
	 * @throws IOException
	 */
	@Test
	public void testGetNextQuery() throws IOException {
		Worker worker = new UPDATEWorker(new String[] {"1", "1", null, "http://dbpedia.org/sparql", null, null,  "5",
				"", "0", "0", "NONE","NONE"});
		((AbstractWorker) worker).setQueriesList(new File[] {new File("src/test/resources/worker/sparql.sparql") });
		StringBuilder query = new StringBuilder();
		StringBuilder queryID = new StringBuilder();
		worker.getNextQuery(query, queryID);
		assertEquals("select bla", query.toString());
		assertEquals("sparql.sparql", queryID.toString());
	}

//	@Test FIXME server mock cannot use credentials, must think about something
	public void checkCredentials() {
		Worker worker = new UPDATEWorker(new String[] {"1", "1", null, "http://dbpedia.org/sparql-auth", "dba", "dba", null,
				"", "0", "0", "NONE","NONE"});
//		double time = (double) worker.executeQuery("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
//				+ "INSERT DATA { <http://example/egbook3> dc:title  \"This is an example title\" }", "2").getExecutionTime();
//		assertTrue(time > 0);
		worker.executeQuery("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
				+ "INSERT DATA { <http://example/egbook3> dc:title  \"This is an example title\" }", "2");
	}
	
	/**
	 * closes the Server Mock
	 * @throws IOException
	 */
	@After
	public void close() throws IOException {
		fastConnection.close();
	}

}
