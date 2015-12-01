package org.aksw.iguana.query;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aksw.iguana.utils.FileHandler;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.lang.SPARQLParser;

/**
 * Provides an handler which converts given query patterns into
 * queries with existing values (or if there aren't values) it will be written in a file
 * called queriesWithNoValues 
 * There will be only generate queries for a querypattern, until limit is reached
 *  
 * @author Felix Conrads
 */
public class QueryHandler {
	
	private static final String FEASIBLE_QUERY_START = "#--";

	public static void main(String[] argc) throws IOException{
		org.apache.log4j.Logger.getLogger("log4j.logger.org.apache.jena.arq.info").setLevel(org.apache.log4j.Level.OFF);
		org.apache.log4j.Logger.getLogger("log4j.logger.org.apache.jena.arq.exec").setLevel(org.apache.log4j.Level.OFF);
		org.apache.log4j.Logger.getLogger("log4j.logger.org.apache.jena").setLevel(org.apache.log4j.Level.OFF);
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		getFeasibleToList(argc[0], Logger.getGlobal());
		
	}
	
	
	/** The con. */
	private Connection con;
	
	/** The path. */
	private String path = "queryvalues"+File.separator;
	
	/** The failed queries. */
	private String failedQueries = "queriesWithNoValues";
	
	private Logger log = Logger.getLogger(QueryHandler.class.getSimpleName());
	
	/** The limit. */
	private int limit = 2000;
	
	/** The file for queries. */
	private String fileForQueries;
	
	/**
	 * Instantiates a new query handler.
	 *
	 * @param con the con
	 * @param fileForQueries the file for queries
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public QueryHandler(Connection con, String fileForQueries) throws IOException{
		this.con = con;
		LogHandler.initLogFileHandler(log, QueryHandler.class.getSimpleName());
		this.fileForQueries = fileForQueries;
	}
	

	/**
	 * NTRIPLE File to an insert or delete query.
	 *
	 * @param file the filename
	 * @param insert if query should be insert (true) or delete (false)
	 * @param graphUri the graph to use (can be null)
	 * @return the query
	 */
	public static String ntToQuery(String file, Boolean insert, String graphUri){
		try {
			return ntToQuery(new File(file), insert, graphUri);
		} catch (IOException e) {
			return null;
		}
	}
	
	/**
	 * NTRIPLE File to an insert or delete query.
	 *
	 * @param file the file
	 * @param insert if query should be insert (true) or delete (false)
	 * @param graphUri the graph to use (can be null)
	 * @return the query
	 * @throws IOException 
	 */
	private static String ntToQuery(File file, Boolean insert, String graphUri) throws IOException{
//		try{
			String query = "";
			query= "INSERT {";
			if(!insert){
				query="DELETE WHERE {";
			}
			if(graphUri!=null){
				query+=" GRAPH <"+graphUri+"> { ";
			}

			FileReader fis = new FileReader(file);
			BufferedReader bis = new BufferedReader(fis);
			String triple="";
			while((triple=bis.readLine())!=null){
				query+=triple;
			}
			bis.close();
			if(graphUri!=null){
				query+=" }";
			}
			query+=" }";
			if(insert)
				query+=" WHERE {?s ?p ?o}";
//			br.close();
//			System.out.println(query);
			return query;
//		}
//		catch(IOException e){
//			e.printStackTrace();
//			return null;
//		}
	}
	
	public static List<String[]> getFeasibleToList(String file, Logger log) throws IOException{
		List<String[]> feasibleList = new LinkedList<String[]>();
		File f = new File(file);
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		String line;
		int index =0;
		String[] add = new String[2];
		String query="";
		String oldLine="";
		while((line=br.readLine())!=null){
			oldLine = line;
			if(line.startsWith(FEASIBLE_QUERY_START)){
				if(!query.isEmpty()){
					try{
						QueryFactory.create(query.replaceAll(" +", " "));
					}
					catch(Exception e){
						index++;
						log.warning("Couldn't create Query: "+query.replaceAll(" +", " "));
						LogHandler.writeStackTrace(log, e, Level.WARNING);
						continue;
					}
					add[0]=query.replaceAll(" +", " ");
					add[1]=index+"";
					index++;
					feasibleList.add(add);
				    add = new String[2];
					query="";
				}
				continue;
			}
			query+=" "+line;
		}
		if(!oldLine.equals(FEASIBLE_QUERY_START)){
			if(!query.isEmpty()){
				try{
					QueryFactory.create(query.replaceAll(" +", " "));
					add[0]=query.replaceAll(" +", " ");
					add[1]=index+"";
					index++;
					feasibleList.add(add);
				}
				catch(Exception e){
					index++;
					log.warning("Couldn't create Query: "+query.replaceAll(" +", " "));
					LogHandler.writeStackTrace(log, e, Level.WARNING);
				}
			}
		}
		br.close();
		return feasibleList;
		
	}
	
	public static List<String[]> getInstancesToList(String file, Logger log) throws IOException{
		List<String[]> feasibleList = new LinkedList<String[]>();
		File f = new File(file);
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		String line;
		int index =0;
		while((line=br.readLine())!=null){
			String[] add = new String[2];
			add[0]=line.replaceAll(" +", " ");
			add[1]=index+"";
			index++;
			feasibleList.add(add);
		}
		br.close();
		return feasibleList;
		
	}
	
	
	/**
	 * Initialization
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void init() throws IOException{
		if(new File(path).exists()){
			log.info("Path "+path+"already exists. Use cached Queries");
		}
		new File(path).mkdir();
		init(fileForQueries);
	}
	
	/**
	 * Sets the path.
	 *
	 * @param path the new path
	 */
	public void setPath(String path){
		this.path = path;
	}
	
	/**
	 * Gets the path.
	 *
	 * @return the path
	 */
	public String getPath(){
		return path;
	}
	
	/**
	 * Gets the absolut path.
	 *
	 * @return the absolut path
	 */
	public String getAbsolutPath(){
		return new File(path+File.separator).getAbsolutePath();
	}
	
	/**
	 * Sets the limit.
	 *
	 * @param i the new limit
	 */
	public void setLimit(int i){
		limit = i;
	}

	
	/**
	 * Initialization of the QueryHandler.
	 * Generating The Queries out of the given Patterns
	 * 
	 *
	 * @param queriesFile the queriePatterns file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void init(String queriesFile) throws IOException{
//		rand = new Random(2);
		File f = new File(failedQueries);
		if(f.exists())
			f.delete();
		//Gets the Values
		List<String> queryPatterns = new LinkedList<String>(FileHandler.getLines(queriesFile)); 
		int i=0;
		for(String p : queryPatterns){
			if(p.isEmpty()){
				continue;
			}
			log.info("Processing query: "+p);
			String test="";
			
			try{
				test = " "+p.toLowerCase().replaceAll("%%v[0-9]*%%", "?v");
				SPARQLParser sp = SPARQLParser.createParser(Syntax.syntaxSPARQL_11);
				sp.parse(QueryFactory.create(), test);
				
				valuesToCSV2(p, String.valueOf(i));		
			}
			catch(QueryParseException e){
//				try{
//					test = " "+p.toLowerCase().replaceAll("%%v[0-9]*%%", "<http://example.com>");
//					UpdateParser up = UpdateParser.createParser(Syntax.syntaxSPARQL_11);
//					UpdateSink sink = new UpdateRequestSink(new UpdateRequest());
//					up.parse(sink, test);
//					updatePattern(p, String.valueOf(i));
//				}
//				catch(QueryParseException e1){
//					Logger.getGlobal().warning("Couldn't validate Query\n"+test+"\n. Following are both Stack traces");
					Logger.getGlobal().warning("SPARQL Parse exception");
					LogHandler.writeStackTrace(Logger.getGlobal(), e, Level.SEVERE);
//					Logger.getGlobal().warning("Update Parse exception");
//					LogHandler.writeStackTrace(Logger.getGlobal(), e1, Level.SEVERE);
//				}
			}
//			if((test.contains(" ask") || test.contains(" select") || test.contains(" construct") || test.contains(" describe"))){
//				//SELECT, ASK, DESCRIBE, CONSTRUCT
////				QueryFactory.create(p);
////				System.out.println(test);
//				valuesToCSV(p, String.valueOf(i));
//			}
//			else{
//				//UPDATE 
//				System.out.println(test);
//				System.out.println(p);
//				updatePattern(p, String.valueOf(i));
//			}
			
			i++;
		}
	}
	
	
	private int valuesToCSV2(String pattern, String fileName){
		
		String query = String.valueOf(pattern);
//		query  = PatternSolution.queryToPattern(query);
		int ret = 0;
		File f = new File(path+File.separator+fileName+".txt");
		PrintWriter pwfailed=null;
		PrintWriter pw =null;
		try{
			new File(path).mkdirs();
			File failed = new File(failedQueries+".txt");
			failed.createNewFile();
			
			f.createNewFile();
			pwfailed= new PrintWriter(new FileOutputStream(failed, true));
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8), true);
			
			String q = selectPattern(query);
			if(!query.matches(".*%%v[0-9]*%%.*")){
				log.info("No Variables are set");
				pw.write(query);
				pw.println();
				pw.close();
				pwfailed.close();
				return 1;
			}
			com.hp.hpl.jena.query.ResultSet m = (com.hp.hpl.jena.query.ResultSet)getModelFromQuery(QueryFactory.create(q));
			
			while(m.hasNext()){
				QuerySolution qs = m.next();
				String line = query;
				for(String varName : m.getResultVars()){
					RDFNode node = qs.get(varName);
					String rep = "%%"+varName+"%%";
					String nodeRep = node.asNode().toString(true);
					if(node.isURIResource()){
						nodeRep = "<"+nodeRep+">";
					}
					line =line.replace(rep, nodeRep);
				}
				pw.println(line);
			}
						
		}
		catch(Exception e){
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			if(pwfailed!=null){
				pwfailed.write(pattern);
				pwfailed.println();
			}
			f.delete();
			return ret;
		}
		finally{
			if(pw!=null)	
				pw.close();
			if(pwfailed!=null)
				pwfailed.close();
		}
		return ret;
		
	}
	
	
	private Object getModelFromQuery(Query q){
		QueryExecution qe = QueryExecutionFactory.createServiceRequest(this.con.getEndpoint(), q);
		switch(q.getQueryType()){
		case Query.QueryTypeAsk: 
			return qe.execAsk();
		case Query.QueryTypeConstruct:
			return qe.execConstruct();
		case Query.QueryTypeDescribe:
			return qe.execDescribe();
		case Query.QueryTypeSelect:
			return qe.execSelect();
		}
		return null;
	}
	
	




	/**
	 * Select pattern.
	 *
	 * @param query the query
	 * @return the string
	 */
	private String selectPattern(String query){
		Pattern regex = Pattern.compile("%%v[0-9]*%%", Pattern.UNICODE_CHARACTER_CLASS);
		Matcher matcher = regex.matcher(query);
		Set<String> vars = new HashSet<String>();
		while(matcher.find()){
			String var = matcher.group();
			query = query.replaceAll(var, "?"+var.replace("%", ""));
			vars.add(var.replace("%", ""));
		}
		if(vars.isEmpty()){
			return query;
		}
		SPARQLParser sp = SPARQLParser.createParser(Syntax.syntaxSPARQL_11);
		Query q = sp.parse(QueryFactory.create(), query);
		q.setLimit(Long.valueOf(limit));
		String select = "SELECT DISTINCT ";
		for(String v : vars){
			select+="?"+v+" ";
			if(q.hasGroupBy()){
				q.addGroupBy("?"+v);
			}
		}
		
		switch(q.getQueryType()){
			case Query.QueryTypeSelect: return typeQuery(q, select, "select");
			case Query.QueryTypeAsk: return typeQuery(q, select, "ask");
			case Query.QueryTypeDescribe: return typeQuery(q, select, "describe");
			case Query.QueryTypeConstruct: return typeQuery(q, select, "construct");
		}
		return query;
	}
	
	/**
	 * Type query.
	 *
	 * @param q the q
	 * @param select the select
	 * @param type the type
	 * @return the string
	 */
	private String typeQuery(Query q, String select, String type){
//		String clause = q.serialize();
//		System.out.println(q.getPrefixMapping().toString());
//		System.out.println(q.getQueryPattern().toString());
		Map<String, String> prefixes = q.getPrefixMapping().getNsPrefixMap();
		String prefix="";
		for(String shortForm : prefixes.keySet()){
			prefix += "PREFIX "+shortForm+": <"+prefixes.get(shortForm)+"> ";
		}
		String clause = q.getQueryPattern().toString();
		Query ret = QueryFactory.create(prefix+select+clause);
		ret.setOffset(q.getOffset());
		ret.setLimit(limit);
		ret.setDistinct(true);
		if(!q.getGraphURIs().isEmpty()){
			for(String graph : q.getGraphURIs())
			ret.addGraphURI(graph);
		}


		return ret.serialize().replace("\n", " ");
	}
	
}
