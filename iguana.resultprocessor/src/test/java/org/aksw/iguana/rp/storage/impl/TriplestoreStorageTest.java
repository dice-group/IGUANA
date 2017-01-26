/**
 * 
 */
package org.aksw.iguana.rp.storage.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Properties;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.data.Triple;
import org.aksw.iguana.rp.utils.ServerMock;
import org.junit.Test;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.SocketConnection;

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

	private String metaExp = "INSERT DATA {\n"+
"  <http://iguana-benchmark.eu/recource/1> <http://iguana-benchmark.eu/properties/experiment> <http://iguana-benchmark.eu/recource/1/1> .\n"+
"  <http://iguana-benchmark.eu/recource/1> <http://www.w3.org/2000/01/rdf-schema#Class> <http://iguana-benchmark.eu/class/Suite> .\n"+
"  <http://iguana-benchmark.eu/recource/1/1> <http://iguana-benchmark.eu/properties/task> <http://iguana-benchmark.eu/recource/1/1/1> .\n"+
"  <http://iguana-benchmark.eu/recource/1/1> <http://iguana-benchmark.eu/properties/dataset> <http://iguana-benchmark.eu/recource/dbpedia> .\n"+
"  <http://iguana-benchmark.eu/recource/1/1> <http://www.w3.org/2000/01/rdf-schema#Class> <http://iguana-benchmark.eu/class/Experiment> .\n"+
"  <http://iguana-benchmark.eu/recource/1/1/1> <http://iguana-benchmark.eu/properties/connection> <http://iguana-benchmark.eu/recource/virtuoso> .\n"+
"  <http://iguana-benchmark.eu/recource/1/1/1> <http://www.w3.org/2000/01/rdf-schema#Class> <http://iguana-benchmark.eu/class/Task> .\n"+
"}";

	private String dataExp = "INSERT DATA {\n"+
"  <http://iguana-benchmark.eu/recource/1/1/1> <http://iguana-benchmark.eu/recource/testMetric> <http://iguana-benchmark.eu/recource/a> .\n"+
"  <http://iguana-benchmark.eu/recource/a> <http://iguana-benchmark.eu/properties/b> \"c\" .\n"+
"}";
	
	@Test
	public void metaTest() throws IOException{
		fastServerContainer = new ServerMock(metaExp);
        fastServer = new ContainerServer(fastServerContainer);
        fastConnection = new SocketConnection(fastServer);
        SocketAddress address1 = new InetSocketAddress(FAST_SERVER_PORT);
        fastConnection.connect(address1);
        
        String host = "http://localhost:8023";
        TriplestoreStorage store = new TriplestoreStorage(host, host);
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
        
        fastConnection.close();
	}

	
	@Test
	public void dataTest() throws IOException{
		fastServerContainer = new ServerMock(dataExp);
        fastServer = new ContainerServer(fastServerContainer);
        fastConnection = new SocketConnection(fastServer);
        SocketAddress address1 = new InetSocketAddress(FAST_SERVER_PORT);
        fastConnection.connect(address1);
        
        String host = "http://localhost:8023";
        TriplestoreStorage store = new TriplestoreStorage(host, host);
        Properties p = new Properties();
        p.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.put(COMMON.METRICS_PROPERTIES_KEY, "testMetric");
	    p.put(COMMON.EXTRA_META_KEY, new Properties());
        
        Triple[] t = new Triple[1];
	    t[0] = new Triple("a", "b", "c");
	    store.addData(p, t);
	    store.commit();
        
        fastConnection.close();
	}

}
