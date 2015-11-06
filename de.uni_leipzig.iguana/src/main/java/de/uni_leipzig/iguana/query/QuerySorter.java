package de.uni_leipzig.iguana.query;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bio_gene.wookie.utils.LogHandler;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.lang.SPARQLParser;
import com.hp.hpl.jena.sparql.lang.UpdateParser;
import com.hp.hpl.jena.sparql.modify.UpdateRequestSink;
import com.hp.hpl.jena.sparql.modify.UpdateSink;
import com.hp.hpl.jena.update.UpdateRequest;

/**
 * The Class QuerySorter helps to differs between SPARQL and UPDATE queries and
 * calculates necessary intervalls and variables for the {@link QueryHandler}
 * 
 * @author Felix Conrads
 */
public class QuerySorter {

	/** The logger. */
	private static Logger log;

	static {
		log = Logger.getLogger(QuerySorter.class.getName());
		log.setLevel(Level.INFO);
		LogHandler.initLogFileHandler(log, QuerySorter.class.getName());
	}


	static int counter = 0;

	public static Query isSPARQL(String query) {
		
		parseSPARQL(query);
		if (SPARQLquery != null) {
			counter = 0;
			return SPARQLquery;
		} else {
			counter = 0;
			return null;
		}

	}

	private static Query SPARQLquery;

	public static void parseSPARQL(String query) {

		if (counter > 30) {
			log.warning("Recursion limit is riched within the query: \n" + query);
			

			SPARQLquery = null;
			return;
		}
		counter++;
		try {
			SPARQLParser sp = SPARQLParser.createParser(Syntax.syntaxSPARQL_11);
			Query q = sp.parse(QueryFactory.create(), query);
			SPARQLquery = q;
		} catch (QueryParseException e) {
			if (e.getMessage().contains("Unresolved prefixed name")) {
				Pattern p = Pattern
						.compile("Unresolved prefixed name: (.*?):(\\w+)");
				Matcher m = p.matcher(e.getMessage());
				String prefix = "";
				String suffix = "";
				if (m.find()) {
					prefix = m.group(1);
					suffix = m.group(2);
				} else {
					log.warning("Query couldn't be parsed: \n" + query);
					LogHandler.writeStackTrace(log, e, Level.WARNING);
					SPARQLquery = null;
				}
				if (prefix.toLowerCase().equals("bif")) {
					query = query.replaceAll("(" + prefix + ":" + suffix + ")",
							"<$1>");
					isSPARQL(query);
				} 
//				else if (CopyOfPrefixes.prefixes.containsKey(prefix
//						.toLowerCase())) {
//					String onePrefix = CopyOfPrefixes.prefixes.get(prefix
//							.toLowerCase());
//					String prefix2 = "PREFIX " + prefix.toLowerCase() + ": <"
//							+ onePrefix + ">\n" + query;
//					isSPARQL(prefix2);
//
//				}

			} else if (e.getMessage().contains("Encountered \" \",\" \", \"\"")) {
				if (query.toLowerCase().contains("select.*?,")) {
					query = query.replaceAll("(\\?\\w+),", "$1");
					isSPARQL(query);
				}else{
					log.warning("Query couldn't be parsed: \n" + query);
					LogHandler.writeStackTrace(log, e, Level.WARNING);
					SPARQLquery = null;
				}
			}
			// else if (e.getMessage().toLowerCase()
			// .contains("encountered \" \"count\"")) {
			// query = query.replaceAll("(?i)(count.*?AS.*?\\?\\w+)",
			// "\\($0\\)");
			// isSPARQL(query);
			// }

			else {
				log.warning("Query couldn't be parsed: \n" + query);
				LogHandler.writeStackTrace(log, e, Level.WARNING);
				SPARQLquery = null;
			}
		}catch (Exception e) {
			log.warning("Query couldn't be parsed: \n" + query);
			LogHandler.writeStackTrace(log, e, Level.WARNING);
			SPARQLquery = null;

		}

	}

	/**
	 * Checks if query is an UPDATE query.
	 *
	 * @param query
	 *            the query
	 * @return true if it is, false otherwise
	 */
	public static Boolean isSPARQLUpdate(String query) {
		try {
			UpdateParser up = UpdateParser.createParser(Syntax.syntaxSPARQL_11);
			UpdateSink sink = new UpdateRequestSink(new UpdateRequest());
			up.parse(sink, query);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
