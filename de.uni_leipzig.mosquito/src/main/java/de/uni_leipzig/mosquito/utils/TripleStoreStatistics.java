package de.uni_leipzig.mosquito.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;



/**
 * Erst ab SPARQL 1.1 möglich
 * Problem: Möglicherweise könnten die Queries nicht schnell genug ausführbar sein, dann muss das über denn RAM laufen
 * 
 * @author Felix Conrads
 *
 */
public class TripleStoreStatistics {

		private static Long query(Connection con, String query){
			Logger log = Logger.getLogger("TripleStoreStatistics");
			try {
				ResultSet result = con.select(query);
				result.next();
				LiteralImpl ret = (LiteralImpl) result.getObject("no");
				Long count = ret.getLong();
				return count;
			} catch ( SQLException | NullPointerException e) {
				LogHandler.writeStackTrace(log, e, Level.WARNING);
			}
			return -1L;
		}
	
		public static Long tripleCount(Connection con, String graphURI){
			//validated with 4store
			String query="SELECT (COUNT(*) AS ?no) "+(graphURI!=null?"FROM <"+graphURI+">":"")+" { ?s ?p ?o  }";
			return query(con, query);
			
		}
		
		public static Long objectNodeCount(Connection con, String graphURI){
			//validated with 4store 
			String query="SELECT (COUNT(DISTINCT ?o) AS ?no) "+(graphURI!=null?"FROM <"+graphURI+">":"")+" WHERE{ ?s ?p ?o  filter (!isLiteral(?o)) }";
			return query(con, query);
		}
		
		public static Long subjectNodeCount(Connection con, String graphURI){
			//validated with 4store 
			String query="SELECT (COUNT(DISTINCT ?s) AS ?no) "+(graphURI!=null?"FROM <"+graphURI+">":"")+" WHERE { ?s ?p ?o  }";
			return query(con, query);
		}
		
		public static Long entityCount(Connection con, String graphURI){
			//validated with 4store 
			String query="SELECT (COUNT(DISTINCT ?s) AS ?no) "+(graphURI!=null?"FROM <"+graphURI+">":"")+" WHERE{ ?s a []  }";
			return query(con, query);
		}
		
		public static Long avgOutDegree(Connection con, Boolean literals, String graphURI){
			String query="";
			if(literals){
				
				//TODO Löschen???
//				query="SELECT (AVG(?count) AS ?no) "+(graphURI!=null?"FROM <"+graphURI+"> ":"")
//						+ "WHERE {SELECT (COUNT(?t) AS ?count) ?s WHERE{ {?s ?q ?t}{SELECT ?s "
//						+ "WHERE {  ?s ?p ?o   }GROUP BY ?s}}}";
				query="SELECT (AVG(?co) AS ?no)  "+(graphURI!=null?"FROM <"+graphURI+"> ":"")
						+ " WHERE { SELECT (SAMPLE(?s) AS ?NAME) (COUNT(?t) AS ?co) WHERE  { ?s ?p ?t } GROUP BY ?s}";
			}else{
				
				//TODO Löschen???
//				query="SELECT (AVG(?count) AS ?no) "+(graphURI!=null?"FROM <"+graphURI+"> ":"")
//						+ "WHERE {SELECT (COUNT(?s) AS ?count) ?s WHERE{ {?s ?q ?t  filter(!isLiteral(?t))}{SELECT ?s "
//						+ "WHERE {  ?s ?p ?o   }GROUP BY ?s}}}";
				query="SELECT (AVG(?co) AS ?no)  "+(graphURI!=null?"FROM <"+graphURI+"> ":"")
						+ " WHERE { SELECT (SAMPLE(?s) AS ?NAME) (COUNT(?t) AS ?co) "+
						"WHERE  { ?s ?p ?t filter(!isLiteral(?t))} GROUP BY ?s}";
			}
			return query(con, query);
		}
		
		public static Long avgInDegree(Connection con, Boolean literals, String graphURI){
			String query="";
			if(literals){
				query="SELECT (AVG(?co) AS ?no) "+(graphURI!=null?"FROM <"+graphURI+"> ":"")
						+ " WHERE { SELECT (SAMPLE(?s) AS ?NAME) (COUNT(?t) AS ?co) WHERE  { ?t ?p ?o } GROUP BY ?o}";
			}else{
				query="SELECT (AVG(?co) AS ?no) "+(graphURI!=null?"FROM <"+graphURI+"> ":"")
						+ "WHERE { SELECT (SAMPLE(?s) AS ?NAME) (COUNT(?t) AS ?co) "
						+ "WHERE  { ?t ?p ?o filter(!isLiteral(?o)) } GROUP BY ?o}";
			}
			return query(con, query);
		}
		
	
}
