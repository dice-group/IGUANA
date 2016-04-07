package org.aksw.iguana.utils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.aksw.iguana.query.QueryHandler;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.connection.ConnectionFactory;
import org.bio_gene.wookie.utils.ConfigParser;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class ConnectionTest {
	
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, SQLException{
		ConfigParser cp = ConfigParser.getParser(args[0]);
		Element rootNode = cp.getElementAt("iguana", 0);
		Element dbNode = cp.getElementAt("databases", 0);
		Map<String, String> config = Config.getParameter(rootNode);
		List<String> databaseIds = Config.getDatabaseIds(rootNode,
				Config.DBTestType.valueOf(config.get("dbs")), config.get("ref"), null);
		
		for(String db : databaseIds){
			Connection con = ConnectionFactory.createConnection(dbNode, db);

			//testing
			con.update("DROP SILENT GRAPH <http://test.com>");

			System.out.println("testing upload");
			con.uploadFile("000001.added.nt", "http://test.com");
			java.sql.ResultSet res = con.select("SELECT * FROM <http://test.com> {?s ?p ?o}");
			if(res.next()){
				String a = res.getString(1);
				String b = res.getString(2);
				String c = res.getString(3);
				if(!a.equals("http://x.x")){
					
					System.out.println("ERROR: Couldn't upload: "+a+" "+b+" "+c);
					
				}
				else if(!b.equals("http://y.y")){
					System.out.println("ERROR: Couldn't upload: "+a+" "+b+" "+c);
				}
				else if(!c.equals("http://z.z")){
					System.out.println("ERROR: Couldn't upload: "+a+" "+b+" "+c);
					
				}
				else{
					System.out.println("Upload works");
				}
			}
			else{
				System.out.println("ERROR Couldnt upload");
			}
			System.out.println("testing delete");
			QueryHandler.ntToQuery("000001.removed.nt", false, "http://test.com");
			con.deleteFile("000001.removed.nt", "http://test.com");
			res = con.select("SELECT * FROM <http://test.com> {?s ?p ?o}");
			if(res.next()){
				System.out.println("ERROR: Couldn'T delte");
			}
			else{
				System.out.println("Deletion works");
			}
			res.close();
				
			con.update("DROP SILENT GRAPH <http://test.com>");
		}
	}
	
}
