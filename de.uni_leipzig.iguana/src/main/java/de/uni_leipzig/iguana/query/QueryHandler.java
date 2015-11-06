package de.uni_leipzig.iguana.query;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.connection.ConnectionFactory;
import org.bio_gene.wookie.utils.GraphHandler;
import org.bio_gene.wookie.utils.LogHandler;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.lang.SPARQLParser;
import com.hp.hpl.jena.sparql.lang.UpdateParser;
import com.hp.hpl.jena.sparql.modify.UpdateRequestSink;
import com.hp.hpl.jena.sparql.modify.UpdateSink;
import com.hp.hpl.jena.update.UpdateRequest;

import de.uni_leipzig.iguana.data.TripleStoreHandler;
import de.uni_leipzig.iguana.utils.FileHandler;
import de.uni_leipzig.iguana.utils.RandomStringBuilder;

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
		QueryHandler qh = new QueryHandler(ConnectionFactory.createImplConnection("dbpedia.org/sparql", "dbpedia.org/sparql", 300), "pattern.txt");
		qh.init();
		
		
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
	
	/** The rand. */
	private Random rand;
	
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
	public static String ntToQuery(File file, Boolean insert, String graphUri) throws IOException{
//		try{
			String query = "";
			query= "INSERT {";
			if(!insert){
				query="DELETE WHERE {";
			}
			if(graphUri!=null){
				query+=" GRAPH <"+graphUri+"> { ";
			}
//			Model m = ModelFactory.createDefaultModel();
//			String uriFile="";
//			try {
//				uriFile = file.toURI().normalize().toURL().toString();
//			} catch (MalformedURLException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//				//			m.read(uriFile);
//			try {
//				m.read(uriFile);
//			} catch (Exception e) {
//				Logger.getGlobal().severe("Problems with model");
//				return "";
//			}
//			String lines = GraphHandler.GraphToSPARQLString(m.getGraph());
//			lines = lines.substring(1, lines.length()-1);
////			FileInputStream fis = new FileInputStream(file);
////			BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
////			String line ="";
////			while((line=br.readLine()) != null){
////				if(line.isEmpty() || line.equals("\n")){
////					continue;
////				}
////				query +="";
////				query +=line;
////				query +=" . ";
////			}
////			query = query.substring(0, query.lastIndexOf("."));
//			query+=lines;
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
			System.out.println(query);
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
		while((line=br.readLine())!=null){
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
		rand = new Random(2);
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
				try{
					test = " "+p.toLowerCase().replaceAll("%%v[0-9]*%%", "<http://example.com>");
					UpdateParser up = UpdateParser.createParser(Syntax.syntaxSPARQL_11);
					UpdateSink sink = new UpdateRequestSink(new UpdateRequest());
					up.parse(sink, test);
					updatePattern(p, String.valueOf(i));
				}
				catch(QueryParseException e1){
					Logger.getGlobal().warning("Couldn't validate Query\n"+test+"\nneither as SPARQL nor Update. Following are both Stack traces");
					Logger.getGlobal().warning("SPARQL Parse exception");
					LogHandler.writeStackTrace(Logger.getGlobal(), e, Level.SEVERE);
					Logger.getGlobal().warning("Update Parse exception");
					LogHandler.writeStackTrace(Logger.getGlobal(), e1, Level.SEVERE);
				}
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
	 * Values to csv.
	 *
	 * @param pattern the pattern
	 * @param fileName the file name
	 * @return the int
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("unused")
	private int valuesToCSV(String pattern, String fileName) throws IOException{
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
			ResultSet res =null;
			if(!query.matches(".*%%v[0-9]*%%.*")){
				log.info("No Variables are set");
				pw.write(query);
				pw.println();
				pw.close();
				pwfailed.close();
				return 1;
			}
			else{
				for(int i=0;i<10;i++){
					con.execute(q);
				}
				
				res= con.execute(q);
			}
			Boolean result= false, hasPrefixes=true,notFirst=false;
			while(res==null&&(PatternSolution.hasIRIs(query)||PatternSolution.hasPrefixes(query))){
				log.info("trying again...");
				if(PatternSolution.hasIRIs(query))	
					query  = PatternSolution.replaceNextIRI(query);
				else
					query = PatternSolution.nextPrefixToVar(query);
				log.info("With query: "+query);
				q = selectPattern(query);
				for(int i=0;i<10;i++){
					con.execute(q);
				}
				res= con.execute(q);
				System.out.println(res);
			}
			if(res!=null){
				while(hasPrefixes){
					if(!result){
						if(notFirst){
							log.info("trying again...");
							//try to redo
							Boolean iri = false;
							if(PatternSolution.hasIRIs(query))
								iri = true;
							else if(!PatternSolution.hasPrefixes(query)){
								hasPrefixes=false;
								break;
							}
							if(iri)
								query = PatternSolution.replaceNextIRI(query);
							else
								query = PatternSolution.nextPrefixToVar(query);
							q = selectPattern(query);
							log.info("with: "+query+"\nas "+q);
							for(int i=0;i<10;i++){
								con.execute(q);
							}
							res= con.execute(q);
							if(res==null)
								continue;
						}
					}
					notFirst=true;
					while(res.next()){
						result=true;
						hasPrefixes=false;
						ResultSetMetaData rsmd = res.getMetaData();
						int columns = rsmd.getColumnCount();
						String line ="";
						List<Object> vars = new LinkedList<Object>();
						List<String> varMap = new LinkedList<String>();
						for(int i=1 ;i<=columns;i++ ){
							String current = res.getObject(i).toString();
//							com.hp.hpl.jena.query.ResultSet r = (com.hp.hpl.jena.query.ResultSet) res;
//							RDFNode n = r.nextSolution().get("v");
							
							
							System.out.println(NodeFactory.createLiteral("Camp", "en", null).getLiteralLexicalForm());
//							res.get
//							  t = ((TripleIteratorResults)res).getObject(i);		
							if(current==null){
								vars.add("null");
								continue;
							}
							Node cur = TripleStoreHandler.implToNode(current);
							vars.add(GraphHandler.NodeToSPARQLString(cur));
							varMap.add(rsmd.getColumnLabel(i));
						}
						line = patternToQuery(pattern, vars, varMap);
						pw.write(line);
						pw.println();
						ret++;
						hasPrefixes=false;
					}
					res.getStatement().close();
				
				}
//				res.getStatement().close();
			}
			else{
				log.severe("Result of "+con.getEndpoint()+" is null for "+pattern+"\n"+q);
			}
			
			if(!result){
				log.info("Has no Results... writing the pattern to the file");
				String newQuery=pattern;
				Pattern p  =Pattern.compile("%%v[0-9]*%%");
				Matcher m = p.matcher(pattern);
				while(m.find()){
					String group = m.group();
					newQuery = newQuery.replace(group, "?"+group.replace("%", ""));
				}
				pw.println(newQuery);
				pwfailed.write(pattern);
				pwfailed.println();
//				f.delete();
			}
			
			return ret;
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
		
	}


	/**
	 * Pattern to query.
	 *
	 * @param pattern the pattern
	 * @param vars the vars
	 * @return the string
	 */
	private String patternToQuery(String pattern, List<Object> vars, List<String> varMap){
		String query = String.valueOf(pattern);
		Pattern regex = Pattern.compile("%%v[0-9]*%%", Pattern.UNICODE_CHARACTER_CLASS);
		Matcher matcher = regex.matcher(pattern);
//		int i=0;
		List<String> replaced = new LinkedList<String>();
		while(matcher.find()){
			String var = matcher.group();
			String varEsc = var.replace("%", "");
			if(!replaced.contains(var)){
				query = query.replace(var, vars.get(varMap.indexOf(varEsc)).toString());		
//				i++;
				replaced.add(var);
			}
		}
		
		return query;
	}
	
	/**
	 * Update pattern.
	 *
	 * @param pattern the pattern
	 * @param fileName the file name
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private String updatePattern(String pattern, String fileName) throws IOException{
		String query = String.valueOf(pattern);
		RandomStringBuilder rsb = new RandomStringBuilder(100);
		new File(path).mkdirs();
		File f = new File(path+fileName+".txt");
		f.createNewFile();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8), true);
		for(int i=0;i<limit;i++){
			Pattern regex = Pattern.compile("%%v[0-9]*%%", Pattern.UNICODE_CHARACTER_CLASS);
			Matcher matcher = regex.matcher(pattern);
			while(matcher.find()){
				String var = matcher.group();
				if(PatternSolution.mustBeResource(query, var)){
					query = query.replace(var, "<http://example.com/"+rsb.buildString(15)+">");
				}
				else{
					//i=0;d=1;b=2;s=3
					int type = rand.nextInt(4);
					if(type==0)
						query = query.replace(var, String.valueOf(rand.nextInt()));
					else if(type==1)
						query = query.replace(var, String.valueOf(rand.nextDouble()));
					else if(type==2)
						query = query.replace(var, String.valueOf(rand.nextBoolean()));
					else if(type==3)
						query = query.replace(var, "'"+rsb.buildString(15)+"'");
				}
			}
			pw.println(query);
			query = String.valueOf(pattern);
		}
		pw.close();
		
		return query;
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
