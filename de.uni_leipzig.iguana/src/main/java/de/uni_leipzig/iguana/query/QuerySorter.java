package de.uni_leipzig.iguana.query;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
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

import de.uni_leipzig.iguana.clustering.LogSolution;
import de.uni_leipzig.iguana.utils.FileHandler;

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
		log = Logger.getLogger(LogSolution.class.getName());
		log.setLevel(Level.INFO);
		LogHandler.initLogFileHandler(log, "LogClusterQueries");
	}
	/**
	 * Gets the x.
	 *
	 * @param selects
	 *            the selects
	 * @param inserts
	 *            the inserts
	 * @return the x
	 */
	public static double getX(int selects, int inserts) {
		return Math.max(selects, inserts) / (Math.min(selects, inserts) * 1.0);
	}

	/**
	 * Gets the round x.
	 *
	 * @param selects
	 *            the selects
	 * @param inserts
	 *            the inserts
	 * @return the round x
	 */
	public static int getRoundX(int selects, int inserts) {
		return (int) getX(selects, inserts);
	}

	/**
	 * Gets the lambda.
	 *
	 * @param selects
	 *            the selects
	 * @param inserts
	 *            the inserts
	 * @param mu
	 *            the mu
	 * @return the lambda
	 */
	public static double getLambda(int selects, int inserts, double mu) {
		return (Math.pow((selects - mu), 2) + Math.pow((inserts - mu), 2)) / 2;
	}

	/**
	 * Gets the sig.
	 *
	 * @param selects
	 *            the selects
	 * @param inserts
	 *            the inserts
	 * @return the sig
	 */
	public static double getSig(int selects, int inserts) {
		return Math.sqrt(getX(selects, inserts));
	}

	/**
	 * Gets the round sig.
	 *
	 * @param selects
	 *            the selects
	 * @param inserts
	 *            the inserts
	 * @return the round sig
	 */
	public static int getRoundSig(int selects, int inserts) {
		return (int) getSig(selects, inserts);
	}

	/**
	 * Gets the intervall.
	 *
	 * @param selects
	 *            the selects
	 * @param inserts
	 *            the inserts
	 * @return the intervall
	 */
	public static int[] getIntervall(int selects, int inserts) {
		int roundX = getRoundX(selects, inserts);
		int roundSD = getRoundSig(selects, inserts);
		int[] intervall = { roundX, roundX };
		intervall[0] = roundX - roundSD;
		intervall[1] = roundX + roundSD;
		return intervall;
	}

	/**
	 * Gets the intervall.
	 *
	 * @param queriesPath
	 *            the queries path
	 * @return the intervall
	 */
	public static int[] getIntervall(String queriesPath) {
		int[] q = getSelectAndInsertCounts(queriesPath);
		return getIntervall(q[0], q[1]);
	}

	/**
	 * Gets the no of selects and inserts. [noOfSelects, noOfInserts]
	 *
	 * @param queriesPath
	 *            the queries path
	 * @return the select and insert counts
	 */
	@SuppressWarnings("unused")
	public static int[] getSelectAndInsertCounts(String queriesPath) {
		int[] ret = { 0, 0 };
		for (String file : getSPARQL(queriesPath)) {
			ret[0] += 1;
		}
		for (String file : getSPARQLUpdate(queriesPath)) {
			ret[1] += 1;
		}
		return ret;
	}

	/**
	 * Checks if query is a SPARQL query.
	 *
	 * @param query
	 *            the query
	 * @return true if it is, false otherwise
	 */
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
				} else if (CopyOfPrefixes.prefixes.containsKey(prefix
						.toLowerCase())) {
					String onePrefix = CopyOfPrefixes.prefixes.get(prefix
							.toLowerCase());
					String prefix2 = "PREFIX " + prefix.toLowerCase() + ": <"
							+ onePrefix + ">\n" + query;
					isSPARQL(prefix2);

				}

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

	/**
	 * Gets all SPARQL queries of a given path
	 *
	 * @param queriesPath
	 *            the path with query files in it
	 * @return the sparql queries
	 */
	public static List<String> getSPARQL(String queriesPath) {
		List<String> sparqlFiles = new LinkedList<String>();
		for (File f : new File(queriesPath).listFiles()) {
			if (isSPARQL(FileHandler.getLineAt(f, 0)) != null) {
				sparqlFiles.add(f.getName());
			}

		}
		return sparqlFiles;
	}

	/**
	 * Gets all UPDATE queries of a given path
	 *
	 * @param queriesPath
	 *            the path with query files in it
	 * @return the update queries
	 */
	public static List<String> getSPARQLUpdate(String queriesPath) {
		List<String> sparqlFiles = new LinkedList<String>();
		for (File f : new File(queriesPath).listFiles()) {
			if (isSPARQLUpdate(FileHandler.getLineAt(f, 0))) {
				sparqlFiles.add(f.getName());
			}

		}
		return sparqlFiles;
	}
}
