package org.aksw.iguana.test.utils;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.connection.ConnectionFactory;
import org.junit.Test;


public class ConnectionTest {

	@Test
	public void ResultSetOwlimTest() throws SQLException{
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
		ConnectionFactory.setDriver("org.apache.jena.jdbc.remote.RemoteEndpointDriver");
	ConnectionFactory.setJDBCPrefix("jdbc:jena:remote:query=");
//		Connection con = ConnectionFactory.createImplConnection("localhost:8080/openrdf-workbench/repositories/owlim-lite/query", 
//				"localhost:8080/openrdf-workbench/repositories/owlim-lite/update", -1);
//		Connection con = ConnectionFactory.createImplConnection("localhost:9999/bigdata/sparql", null, -1);
		Connection con = ConnectionFactory.createImplConnection("http://localhost:8891/sparql", null, -1);
		//		Connection con = ConnectionFactory.createImplConnection("dbpedia.org/sparql", null, -1);
		ResultSet res = con.select("SELECT * {<http://example/book1> ?p ?o}");
//		int i =0;
		while(res.next()){
			System.out.println(res.getString(1));
			System.out.println(res.getString(2));
			System.out.println("#--------------------#");
//			i++;
		}
		res.close();
//		i=0;
		System.out.println("#############################");
		res = con.select("SELECT * FROM <http://dbpedia.org> {<http://example/book1> ?p ?o}");
		while(res.next()){
			System.out.println(res.getString(1));
			System.out.println(res.getString(2));
			System.out.println("#--------------------#");
//			i++;
		}
		res.close();
		System.out.println("#############################");
		res = con.select("SELECT * FROM <http://dbpedia2.org> {<http://example/book1> ?p ?o}");
		while(res.next()){
			System.out.println(res.getString(1));
			System.out.println(res.getString(2));
			System.out.println("#--------------------#");
//			i++;
		}
		res.close();	
	}
	
	@Test
	public void testDelete(){
		Connection  con = ConnectionFactory.createImplConnection("http://localhost:8891/sparql", "dba", "dba", "http://localhost:8891/sparql-auth", -1);
//		con.uploadFile("src/test/resources/dataset.nt", "http://urn.bla");
		con.deleteFile("src/test/resources/dataset.nt","http://urn.bla");
	}
}
