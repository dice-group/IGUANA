package org.aksw.iguana.test.generation;

import org.aksw.iguana.connection.ConnectionFactory;
import org.junit.Test;

public class CoherenceMetricsTest {

	
	@Test
	public void testConnection(){
		org.apache.log4j.Logger.getLogger(
				"log4j.logger.org.apache.jena.arq.info").setLevel(
				org.apache.log4j.Level.OFF);
		org.apache.log4j.Logger.getLogger(
				"log4j.logger.org.apache.jena.arq.exec").setLevel(
				org.apache.log4j.Level.OFF);
		org.apache.log4j.Logger.getLogger("log4j.logger.org.apache.jena")
				.setLevel(org.apache.log4j.Level.OFF);
		org.apache.log4j.Logger.getRootLogger().setLevel(
				org.apache.log4j.Level.OFF);
		ConnectionFactory
				.setDriver("org.apache.jena.jdbc.remote.RemoteEndpointDriver");
		ConnectionFactory.setJDBCPrefix("jdbc:jena:remote:query=http://");
//		Connection con = ConnectionFactory.createImplConnection("dbpedia.org/sparql", null, -1);

//		DataProducer.writeData("sortedDataset.nt", "dataProd.test.txt", "http://pivot_test_data/campsites", 0.7, 0.9);
	}
}
