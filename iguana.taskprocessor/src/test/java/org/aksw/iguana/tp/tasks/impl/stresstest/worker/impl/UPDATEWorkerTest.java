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

	private ServerMock mock;
	private ContainerServer fastServer;
	private SocketConnection fastConnection;
	private InetSocketAddress address1;
	private String host = "http://localhost:8023";


	/**
	 * @throws IOException
	 */
	public UPDATEWorkerTest() throws IOException {
		mock = new ServerMock();
		fastServer = new ContainerServer(mock);
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
		Worker worker = new UPDATEWorker("1", 1, null, host, null,
				"", 0, 0, "NONE", null);
		long time = worker.getTimeForQueryMs("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
				+ "INSERT DATA { <http://example/egbook3> dc:title  \"This is an example title\" }", "1");
		assertTrue(time > 0);
	}

	/**
	 * Tests if the timeout works
	 * @throws IOException
	 */
	@Test
	public void testTimeout() throws IOException {
		Worker worker = new UPDATEWorker("1", 1, null, host,  1l, 
				"", 0, 0, "NONE",null);
		long time = worker.getTimeForQueryMs("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
				+ "INSERT DATA { <http://example/egbook3> dc:title  \"This is an example title\" }", "1");
		assertEquals(-1, time);
	}

	/**
	 * Tests the getNextQuery method
	 * @throws IOException
	 */
	@Test
	public void testGetNextQuery() throws IOException {
		Worker worker = new UPDATEWorker("1", 1, null, "http://dbpedia.org/sparql", 5l,
				"", 0, 0, "NONE", null);
		((AbstractWorker) worker).setQueriesList(new File[] {new File("src/test/resources/worker/sparql.sparql") });
		StringBuilder query = new StringBuilder();
		StringBuilder queryID = new StringBuilder();
		worker.getNextQuery(query, queryID);
		assertEquals("select bla", query.toString());
		assertEquals("sparql.sparql", queryID.toString());
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
