package de.uni_leipzig.iguana.query;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.lang.SPARQLParser;
import com.hp.hpl.jena.sparql.lang.UpdateParser;
import com.hp.hpl.jena.sparql.modify.UpdateRequestSink;
import com.hp.hpl.jena.sparql.modify.UpdateSink;
import com.hp.hpl.jena.update.UpdateRequest;

import de.uni_leipzig.iguana.utils.FileHandler;

/**
 * The Class QuerySorter helps to differs between SPARQL and UPDATE queries and
 * calculates necessary intervalls and variables for the {@link QueryHandler}
 * 
 * @author Felix Conrads
 */
public class QuerySorter {

	
	/**
	 * Gets the x.
	 *
	 * @param selects the selects
	 * @param inserts the inserts
	 * @return the x
	 */
	public static double getX(int selects, int inserts){
		return Math.max(selects, inserts)/(Math.min(selects,inserts)*1.0);
	}
	
	/**
	 * Gets the round x.
	 *
	 * @param selects the selects
	 * @param inserts the inserts
	 * @return the round x
	 */
	public static int getRoundX(int selects, int inserts){
		return (int) getX(selects, inserts);
	}
	
	/**
	 * Gets the lambda.
	 *
	 * @param selects the selects
	 * @param inserts the inserts
	 * @param mu the mu
	 * @return the lambda
	 */
	public static double getLambda(int selects, int inserts, double mu){
		return (Math.pow((selects-mu), 2)+Math.pow((inserts-mu), 2))/2;
	}
	
	/**
	 * Gets the sig.
	 *
	 * @param selects the selects
	 * @param inserts the inserts
	 * @return the sig
	 */
	public static double getSig(int selects, int inserts){
		return Math.sqrt( getX(selects, inserts));
	}
	
	/**
	 * Gets the round sig.
	 *
	 * @param selects the selects
	 * @param inserts the inserts
	 * @return the round sig
	 */
	public static int getRoundSig(int selects, int inserts){
		return (int) getSig(selects, inserts);
	}
	
	/**
	 * Gets the intervall.
	 *
	 * @param selects the selects
	 * @param inserts the inserts
	 * @return the intervall
	 */
	public static int[] getIntervall(int selects, int inserts){
		int roundX = getRoundX(selects, inserts);
		int roundSD = getRoundSig(selects, inserts);
		int[] intervall = {roundX, roundX};
		intervall[0] =  roundX-roundSD;
		intervall[1] =  roundX+roundSD;
		return intervall;
	}
	
	/**
	 * Gets the intervall.
	 *
	 * @param queriesPath the queries path
	 * @return the intervall
	 */
	public static int[] getIntervall(String queriesPath){
		int[] q = getSelectAndInsertCounts(queriesPath);
		return getIntervall(q[0], q[1]);
	}
	
	/**
	 * Gets the no of selects and inserts.
	 * [noOfSelects, noOfInserts]
	 *
	 * @param queriesPath the queries path
	 * @return the select and insert counts
	 */
	@SuppressWarnings("unused")
	public static int[] getSelectAndInsertCounts(String queriesPath){
		int[] ret = {0, 0};
		for(String file : getSPARQL(queriesPath)){
			ret[0] += 1;
		}
		for(String file : getSPARQLUpdate(queriesPath)){
			ret[1] += 1;
		}
		return ret;
	}
	
	/**
	 * Checks if query is a SPARQL query.
	 *
	 * @param query the query
	 * @return true if it is, false otherwise
	 */
	public static Boolean isSPARQL(String query){
		try{
			SPARQLParser sp = SPARQLParser.createParser(Syntax.syntaxSPARQL_11);
			sp.parse(QueryFactory.create(), query);
			return true;
		}
		catch(Exception e){
			return false;
		}
	}
	
	/**
	 * Checks if query is an UPDATE query.
	 *
	 * @param query the query
	 * @return true if it is, false otherwise
	 */
	public static Boolean isSPARQLUpdate(String query){
		try{
			UpdateParser up = UpdateParser.createParser(Syntax.syntaxSPARQL_11);
			UpdateSink sink = new UpdateRequestSink(new UpdateRequest());
			up.parse(sink, query);
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}
	
	/**
	 * Gets all SPARQL queries of a given path
	 *
	 * @param queriesPath the path with query files in it
	 * @return the sparql queries 
	 */
	public static List<String> getSPARQL(String queriesPath){
		List<String> sparqlFiles = new LinkedList<String>();
		for(File f : new File(queriesPath).listFiles()){
			if(isSPARQL(FileHandler.getLineAt(f,0))){
				sparqlFiles.add(f.getName());
			}
			
		}
		return sparqlFiles;
	}
	
	/**
	 * Gets all UPDATE queries of a given path
	 *
	 * @param queriesPath the path with query files in it
	 * @return the update queries 
	 */
	public static List<String> getSPARQLUpdate(String queriesPath){
		List<String> sparqlFiles = new LinkedList<String>();
		for(File f : new File(queriesPath).listFiles()){
			if(isSPARQLUpdate(FileHandler.getLineAt(f,0))){
				sparqlFiles.add(f.getName());
			}
			
		}
		return sparqlFiles;
	}
}
