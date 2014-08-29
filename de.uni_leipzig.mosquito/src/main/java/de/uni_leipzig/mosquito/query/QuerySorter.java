package de.uni_leipzig.mosquito.query;

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

import de.uni_leipzig.mosquito.utils.FileHandler;

public class QuerySorter {

	public static void main(String[] args){
		String select = "SELECT ?s FROM <asd> WHERE {?s ?p ?o}";
		String insert = "INSERT DATA {<a> <b> <d>}";
		String drop = "DROP GRAPH <asd>";
		String bla = "asd";
		System.out.println("select: sparql: "+isSPARQL(select));
		System.out.println("insert: sparql: "+isSPARQL(insert));
		System.out.println("drop: sparql: "+isSPARQL(drop));
		System.out.println("bla: sparql: "+isSPARQL(bla));
		System.out.println("select: sparqlu: "+isSPARQLUpdate(select));
		System.out.println("insert: sparqlu: "+isSPARQLUpdate(insert));
		System.out.println("drop: sparqlu: "+isSPARQLUpdate(drop));
		System.out.println("bla: sparqlu: "+isSPARQLUpdate(bla));
	}
	
	
	public static double getX(int selects, int inserts){
		return Math.max(selects, inserts)/(Math.min(selects,inserts)*1.0);
	}
	
	public static int getRoundX(int selects, int inserts){
		return (int) getX(selects, inserts);
	}
	
	public static double getLambda(int selects, int inserts, double mu){
		return (Math.pow((selects-mu), 2)+Math.pow((inserts-mu), 2))/2;
	}
	
	public static double getSig(int selects, int inserts){
		return Math.sqrt( getX(selects, inserts));
	}
	
	public static int getRoundSig(int selects, int inserts){
		return (int) getSig(selects, inserts);
	}
	
	public static int[] getIntervall(int selects, int inserts){
		int roundX = getRoundX(selects, inserts);
		int roundSD = getRoundSig(selects, inserts);
		int[] intervall = {roundX, roundX};
		intervall[0] =  roundX-roundSD;
		intervall[1] =  roundX+roundSD;
		return intervall;
	}
	
	public static int[] getIntervall(String queriesPath){
		int[] q = getSelectAndInsertCounts(queriesPath);
		return getIntervall(q[0], q[1]);
	}
	
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
	
	public static List<String> getSPARQL(String queriesPath){
		List<String> sparqlFiles = new LinkedList<String>();
		for(File f : new File(queriesPath).listFiles()){
			if(isSPARQL(FileHandler.getLineAt(f,0))){
				sparqlFiles.add(f.getName());
			}
			
		}
		return sparqlFiles;
	}
	
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
