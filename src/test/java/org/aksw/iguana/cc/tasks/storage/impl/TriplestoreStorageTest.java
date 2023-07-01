/**
 * 
 */
package org.aksw.iguana.cc.tasks.storage.impl;

import org.aksw.iguana.cc.tasks.stresstest.storage.impl.TriplestoreStorage;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.cc.tasks.ServerMock;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.After;
import org.junit.Test;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.SocketConnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.junit.Assert.assertEquals;

/**
 * Will test if the TriplestoreStorage sends the correct INSERT command to a Mock Server
 * 
 * @author f.conrads
 *
 */
public class TriplestoreStorageTest {

    private static final int FAST_SERVER_PORT = 8023;
	private ServerMock fastServerContainer;
	private ContainerServer fastServer;
	private SocketConnection fastConnection;

	private String metaExp = "INSERT DATA {\n" +
			"  <http://iguana-benchmark.eu/resource/1/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://iguana-benchmark.eu/class/Experiment> .\n" +
			"  <http://iguana-benchmark.eu/resource/1/1> <http://iguana-benchmark.eu/properties/dataset> <http://iguana-benchmark.eu/resource/dbpedia> .\n" +
			"  <http://iguana-benchmark.eu/resource/1/1> <http://iguana-benchmark.eu/properties/task> <http://iguana-benchmark.eu/resource/1/1/1> .\n" +
			"  <http://iguana-benchmark.eu/resource/dbpedia> <http://www.w3.org/2000/01/rdf-schema#label> \"dbpedia\" .\n" +
			"  <http://iguana-benchmark.eu/resource/dbpedia> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://iguana-benchmark.eu/class/Dataset> .\n" +
			"  <http://iguana-benchmark.eu/resource/virtuoso> <http://www.w3.org/2000/01/rdf-schema#label> \"virtuoso\" .\n" +
			"  <http://iguana-benchmark.eu/resource/virtuoso> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://iguana-benchmark.eu/class/Connection> .\n" +
			"  <http://iguana-benchmark.eu/resource/1/1/1> <http://www.w3.org/2000/01/rdf-schema#startDate> \"???\"^^<http://www.w3.org/2001/XMLSchema#dateTime> .\n" +
			"  <http://iguana-benchmark.eu/resource/1/1/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://iguana-benchmark.eu/resource/ClassName> .\n" +
			"  <http://iguana-benchmark.eu/resource/1/1/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://iguana-benchmark.eu/class/Task> .\n" +
			"  <http://iguana-benchmark.eu/resource/1/1/1> <http://iguana-benchmark.eu/properties/connection> <http://iguana-benchmark.eu/resource/virtuoso> .\n" +
			"  <http://iguana-benchmark.eu/resource/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://iguana-benchmark.eu/class/Suite> .\n" +
			"  <http://iguana-benchmark.eu/resource/1> <http://iguana-benchmark.eu/properties/experiment> <http://iguana-benchmark.eu/resource/1/1> .\n" +
			"}";

	private String dataExp = "INSERT DATA {\n"+
"  <http://iguana-benchmark.eu/resource/a> <http://iguana-benchmark.eu/properties/b> \"c\" .\n"+
"}";

	/**
	 * @throws IOException
	 */
	@After
	public void close() throws IOException {
		fastConnection.close();
	}

	/**
	 * @throws IOException
	 */
	@Test
	public void dataTest() throws IOException{
		fastServerContainer = new ServerMock();
        fastServer = new ContainerServer(fastServerContainer);
        fastConnection = new SocketConnection(fastServer);
        SocketAddress address1 = new InetSocketAddress(FAST_SERVER_PORT);
        fastConnection.connect(address1);
        
        String host = "http://localhost:8023";
        TriplestoreStorage store = new TriplestoreStorage(host, host);

	    Model m = ModelFactory.createDefaultModel();
	    m.add(ResourceFactory.createResource(COMMON.RES_BASE_URI+"a"), ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"b") , "c");
	    store.storeResult(m);
        assertEquals(dataExp.trim(),fastServerContainer.getActualContent().trim());
	}

}
