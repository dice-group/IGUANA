package de.uni_leipzig.iguana;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.connection.ConnectionFactory;


public class ConnectionTest {

	
	public void ResultSetOwlimTest() throws SQLException{
		ConnectionFactory.setDriver("org.apache.jena.jdbc.remote.RemoteEndpointDriver");
	ConnectionFactory.setJDBCPrefix("jdbc:jena:remote:query=http://");
		Connection con = ConnectionFactory.createImplConnection("localhost:8080/openrdf-workbench/repositories/SYSTEM/query", 
				"localhost:8080/openrdf-workbench/repositories/SYSTEM/update", -1);
		ResultSet res = con.select("SELECT * WHERE  {?s ?p ?o}");
		res.close();
		res = con.select("SELECT * FROM <http://bla.bla.bla.bla> WHERE  {?s ?p ?o}");
		res.close();
			
	}
}
