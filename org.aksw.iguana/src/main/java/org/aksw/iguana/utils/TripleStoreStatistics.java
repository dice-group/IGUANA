package org.aksw.iguana.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.connection.Connection;
import org.aksw.iguana.connection.ConnectionFactory;
import org.aksw.iguana.utils.logging.LogHandler;

// TODO should be rewritten with offset limit strategy
/**
 * 
 * Pre Defined queries for stastic usage 
 * 
 * @author Felix Conrads
 *
 */
public class TripleStoreStatistics {
	
	public static void main(String[] argc){
		System.out.println(TripleStoreStatistics.tripleCount(ConnectionFactory.createImplConnection("bio-gene.org:8890/sparql", "", 180)));
	}

		/**
		 * Querying the Connection with a Single Result query
		 *
		 * @param con Connection to use
		 * @param query the query to execute
		 * @return the result of the query
		 */
		private static Long query(Connection con, String query){
			Logger log = Logger.getLogger("TripleStoreStatistics");
			try {
				ResultSet result = con.select(query);
				result.next();
				String ret = result.getString("no");
				if(ret.matches("\"?(\\+|-)?[0-9]+\\.?[0-9]*\"?\\^\\^.*")){
					ret = ret.replaceAll("\\^\\^.*", "").replace("\"", "");
				}
				result.getStatement().close();
				Long count = Long.parseLong(ret);
				return count;
			} catch ( SQLException | NullPointerException e) {
				LogHandler.writeStackTrace(log, e, Level.WARNING);
			}
			return -1L;
		}
	
		public static Long tripleCount(Connection con){
			return tripleCount(con, null);
		}
		
		/**
		 * Counts the no of triples in the dataset
		 *
		 * @param con Connection to use
		 * @param graphURI graph to use (can be null)
		 * @return no of triples in the dataset(graph)
		 */
		public static Long tripleCount(Connection con, String graphURI){
			//validated with 4store
			String query="SELECT (COUNT(*) AS ?no) ";
			query+=(graphURI!=null?"FROM <"+graphURI+">":"");
			query+=" { ?s ?p ?o  }";
			return query(con, query);
			
		}
		
		/**
		 * Counts the no of object resources
		 *
		 * @param con Connection to use
		 * @param graphURI the graph to use (can be null)
		 * @return the result
		 */
		public static Long objectNodeCount(Connection con, String graphURI){
			//validated with 4store 
			String query="SELECT (COUNT(DISTINCT ?o) AS ?no) ";
			query+=(graphURI!=null?"FROM <"+graphURI+">":"");
			query+=" WHERE{ ?s ?p ?o  filter (!isLiteral(?o)) }";
			return query(con, query);
		}
		
		/**
		 * Counts the no of subjects
		 *
		 * @param con Connection to use
		 * @param graphURI the graph to use (can be null)
		 * @return the result
		 */
		public static Long subjectNodeCount(Connection con, String graphURI){
			//validated with 4store 
			String query="SELECT (COUNT(DISTINCT ?s) AS ?no) ";
			query+=(graphURI!=null?"FROM <"+graphURI+">":"");
			query+=" WHERE { ?s ?p ?o  }";
			return query(con, query);
		}
		
		/**
		 * Counts the no of Entities
		 *
		 * @param con Connection to use
		 * @param graphURI the graph to use (can be null)
		 * @return the result
		 */
		public static Long entityCount(Connection con, String graphURI){
			//validated with 4store 
			String query="SELECT (COUNT(DISTINCT ?s) AS ?no) ";
			query+=(graphURI!=null?"FROM <"+graphURI+">":"");
			query+=" WHERE{ ?s a []  }";
			return query(con, query);
		}
		
		/**
		 * Avg out degree.
		 *
		 * @param con Connection to use
		 * @param literals the literals
		 * @param graphURI the graph to use (can be null)
		 * @return the result
		 */
		public static Long avgOutDegree(Connection con, Boolean literals, String graphURI){
			String query="SELECT (AVG(?co) AS ?no)  ";
			query+=(graphURI!=null?"FROM <"+graphURI+"> ":"");
			if(literals){
				
				query+="WHERE { SELECT (SAMPLE(?s) AS ?NAME) (COUNT(?t) AS ?co) WHERE  { ?s ?p ?t } GROUP BY ?s}";
			}else{
				
				query+= " WHERE { SELECT (SAMPLE(?s) AS ?NAME) (COUNT(?t) AS ?co) "+
						"WHERE  { ?s ?p ?t filter(!isLiteral(?t))} GROUP BY ?s}";
			}
			return query(con, query);
		}
		
		/**
		 * Avg in degree.
		 *
		 * @param con Connection to use
		 * @param literals the literals
		 * @param graphURI the graph to use (can be null)
		 * @return the result
		 */
		public static Long avgInDegree(Connection con, Boolean literals, String graphURI){
			String query="SELECT (AVG(?co) AS ?no)  ";
			query+=(graphURI!=null?"FROM <"+graphURI+"> ":"");
			if(literals){
				query+=" WHERE { SELECT (SAMPLE(?s) AS ?NAME) (COUNT(?t) AS ?co) WHERE  { ?t ?p ?o } GROUP BY ?o}";
			}else{
				query+="WHERE { SELECT (SAMPLE(?s) AS ?NAME) (COUNT(?t) AS ?co) "
						+ "WHERE  { ?t ?p ?o filter(!isLiteral(?o)) } GROUP BY ?o}";
			}
			return query(con, query);
		}
		
	
}
