package de.uni_leipzig.mosquito.query;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.GraphHandler;
import org.bio_gene.wookie.utils.LogHandler;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.lang.SPARQLParser;
import com.hp.hpl.jena.sparql.lang.UpdateParser;
import com.hp.hpl.jena.sparql.modify.UpdateRequestSink;
import com.hp.hpl.jena.sparql.modify.UpdateSink;
import com.hp.hpl.jena.update.UpdateRequest;

import de.uni_leipzig.mosquito.data.TripleStoreHandler;
import de.uni_leipzig.mosquito.utils.RandomStringBuilder;

public class QueryHandler {
	
		
	public static void main(String args[]) throws IOException{
		String insert = "INSERT DATA {GRAPH %%v%% {%%v1%% %%v2%% %%v%% . %%v1%% %%v3%% %%v%% . %%v1%% %%v2%% %%v4%% . %%v4%% %%v2%% %%v%% . %%v4%% %%v5%% %%v%%}}";
		String select = "select distinct ?s where {?s <http://dbpedia.org/property/einwohner> ?v} LIMIT 10";
		String construct = "CONSTRUCT   { [] ?p ?name } WHERE { %%v1%% ?p ?name }";
		String ask = "PREFIX foaf:<http://xmlns.com/foaf/0.1/ASD>  ASK  { ?x foaf:name  %%v%% }";
		String describe = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> DESCRIBE %%v%%";
		
		
		insert = insert.replaceAll("%%v[0-9]*%%", "<http://bla.com>");
		select = select.replaceAll("%%v[0-9]*%%", "<http://bla.com>");
		construct = construct.replaceAll("%%v[0-9]*%%", "<http://bla.com>");
		describe = describe.replaceAll("%%v[0-9]*%%", "<http://bla.com>");
		ask = ask.replaceAll(" %%v[0-9]*%% ", "<http://bla.com>");
		
		UpdateParser ps11 = UpdateParser.createParser(Syntax.syntaxSPARQL_11);

		SPARQLParser ps10 = SPARQLParser.createParser(Syntax.syntaxSPARQL_11);
		Query q = QueryFactory.create();
		UpdateSink u = new UpdateRequestSink(new UpdateRequest());
		try{
			ps11.parse(u, insert);
		}catch(Exception e){
			e.printStackTrace();
		}
		try{
			u = new UpdateRequestSink(new UpdateRequest());
			ps11.parse(u, "BULLSHIT ");
		}catch(Exception e){
			System.out.println("superst");
		}
		try{
			u = new UpdateRequestSink(new UpdateRequest());
			ps11.parse(u, "LOAD <asd> ");
		}catch(Exception e){
			e.printStackTrace();
		}
		try{
			u = new UpdateRequestSink(new UpdateRequest());
			ps11.parse(u, select);
		}catch(Exception e){
			System.out.println("superst");
		}
		try{
			q = QueryFactory.create();
			System.out.println(ps10.parse(q, insert));
		}catch(Exception e){
			System.out.println("superst");
		}
		try{
			q = QueryFactory.create();
			System.out.println(ps10.parse(q, select));
		}catch(Exception e){
			e.printStackTrace();
		}
		try{
			q = QueryFactory.create();
			System.out.println(ps10.parse(q, construct));
		}catch(Exception e){
			e.printStackTrace();
		}
		try{
			q = QueryFactory.create();
			System.out.println(ps10.parse(q, describe));
		}catch(Exception e){
			e.printStackTrace();
		}
		try{
			q = QueryFactory.create();
			System.out.println(ps10.parse(q, ask));
		}catch(Exception e){
			e.printStackTrace();
		}

		
		
//		PrintWriter pw = new PrintWriter(new File("queries.txt"));
//		pw.write(ask);
//		pw.println();
//		pw.write(construct);
//		pw.println();
//		pw.write(select);
//		pw.println();
//		pw.write(describe);
//		pw.println();
//		pw.write(insert);
//		pw.println("SELECT ?abstract WHERE { <http://dbpedia.org/resource/Ernesto_J._Cordero> <http://dbpedia.org/ontology/abstract> ?abstract. FILTER langMatches(lang(?abstract), %%v%%)}");
//		pw.close();
//		
//		
//		
//		Connection con = ConnectionFactory.createImplConnection("dbpedia.org/sparql");
//		QueryHandler qh = new QueryHandler(con, "queries.txt");
//		qh.init();
	
	}
	
	private Connection con;
	private String path = "queryvalues"+File.separator;
	private String failedQueries = "queriesWithNoValues";
	private int limit = 5000;
	private Random rand;
	private String fileForQueries;
	
	public QueryHandler(Connection con, String fileForQueries) throws IOException{
		this.con = con;
		this.fileForQueries = fileForQueries;
	}
	
	
	public static String ntToQuery(String file, Boolean insert, String graphUri){
		return ntToQuery(new File(file), insert, graphUri);
	}
	
	public static String ntToQuery(File file, Boolean insert, String graphUri){
//		try{
			String query = "";
			query= "INSERT DATA {";
			if(!insert){
				query="DELETE DATA {";
			}
			if(graphUri!=null){
				query+=" GRAPH <"+graphUri+"> { ";
			}
			Model m = ModelFactory.createDefaultModel();
			m.read(file.toURI().toString());
			String lines = GraphHandler.GraphToSPARQLString(m.getGraph());
			lines = lines.substring(1, lines.length()-1);
//			FileInputStream fis = new FileInputStream(file);
//			BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
//			String line ="";
//			while((line=br.readLine()) != null){
//				if(line.isEmpty() || line.equals("\n")){
//					continue;
//				}
//				query +="";
//				query +=line;
//				query +=" . ";
//			}
//			query = query.substring(0, query.lastIndexOf("."));
			query+=lines;
			if(graphUri!=null){
				query+=" }";
			}
			query+=" }";
//			br.close();
			return query;
//		}
//		catch(IOException e){
//			e.printStackTrace();
//			return null;
//		}
	}
	
	public void init() throws IOException{
		new File(path).mkdir();
		init(fileForQueries);
	}
	
	public void setPath(String path){
		this.path = path;
	}
	
	public String getPath(){
		return path;
	}
	
	public String getAbsolutPath(){
		return new File(path+File.separator).getAbsolutePath();
	}
	
	public void setLimit(int i){
		limit = i;
	}

	
	private void init(String queriesFile) throws IOException{
		rand = new Random(2);
		File f = new File(failedQueries);
		if(f.exists())
			f.delete();
		//Gets the Values
		List<String> queryPatterns = Files.readAllLines(Paths.get(queriesFile), Charset.forName("UTF-8")); 
		int i=0;
		for(String p : queryPatterns){
			if(p.isEmpty()){
				continue;
			}
			String test = " "+p.toLowerCase().replaceAll("%%v[0-9]*%%", "<http://example.com>");
			
			try{
				
				SPARQLParser sp = SPARQLParser.createParser(Syntax.syntaxSPARQL_11);
				sp.parse(QueryFactory.create(), test);
				valuesToCSV(p, String.valueOf(i));		
			}
			catch(QueryParseException e){
				try{
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
	
	
	
	private int valuesToCSV(String pattern, String fileName) throws IOException{
		String query = String.valueOf(pattern);
		int ret = 0;
		try{
			new File(path).mkdirs();
			File failed = new File(failedQueries+".txt");
			failed.createNewFile();
			File f = new File(path+fileName+".txt");
			
			f.createNewFile();
			
			PrintWriter pwfailed = new PrintWriter(new FileOutputStream(failed, true));
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8), true);
			String q = selectPattern(query);
			ResultSet res =null;
			if(!QuerySorter.isSPARQL(q)){
				
			}
			else{
				res= con.execute(q);
			}
			Boolean result= false;
			if(res!=null){
				while(res.next()){
					result=true;
					ResultSetMetaData rsmd = res.getMetaData();
					int columns = rsmd.getColumnCount();
					String line ="";
					List<Object> vars = new LinkedList<Object>();
					for(int i=1 ;i<=columns;i++ ){
						Object current = res.getObject(i);
						if(current==null){
							vars.add("null");
							continue;
						}
						Node cur = TripleStoreHandler.implToNode(current);
						vars.add(GraphHandler.NodeToSPARQLString(cur));
					}
					line = patternToQuery(pattern, vars);
					pw.write(line);
					pw.println();
					ret++;
				}
			}
			pw.close();
			if(!result){
				pwfailed.write(pattern);
				pwfailed.println();
				f.delete();
			}
			pwfailed.close();
			return ret;
		}
		catch(Exception e){
			e.printStackTrace();
			return ret;
		}
		
	}


	private String patternToQuery(String pattern, List<Object> vars){
		String query = String.valueOf(pattern);
		Pattern regex = Pattern.compile("%%v[0-9]*%%", Pattern.UNICODE_CHARACTER_CLASS);
		Matcher matcher = regex.matcher(pattern);
		int i=0;
		List<String> replaced = new LinkedList<String>();
		while(matcher.find()){
			String var = matcher.group();
			if(!replaced.contains(var)){
				//TODO!!!
				query = query.replace(var, vars.get(i).toString());		
				i++;
				replaced.add(var);
			}
		}
		
		return query;
	}
	
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

		Query q = QueryFactory.create(query);
		q.setLimit(Long.valueOf(limit));
		String select = "SELECT DISTINCT ";
		for(String v : vars){
			select+="?"+v+" ";
		}
		
		switch(q.getQueryType()){
			case Query.QueryTypeSelect: return typeQuery(q, select, "select");
			case Query.QueryTypeAsk: return typeQuery(q, select, "ask");
			case Query.QueryTypeDescribe: return typeQuery(q, select, "describe");
			case Query.QueryTypeConstruct: return typeQuery(q, select, "construct");
		}
		return query;
	}
	
	private String typeQuery(Query q, String select, String type){
		String clause = q.serialize();
		int i = clause.toLowerCase().indexOf(type);
		String prefix = clause.substring(0, i);
		clause = clause.substring(i);
		i = clause.indexOf('\n');
		clause = clause.substring(i);
		if(type.equals("construct")){
			clause = clause.substring(1);
			i = clause.indexOf('\n');
			clause = clause.substring(i);
		}
		if(type.equals("describe") && !clause.contains("WHERE")){
			return "SELECT DISTINCT ?g WHERE {GRAPH ?g {?s ?p ?o}}";
		}
		return (prefix+select+clause).replace("\n"," ");
	}
	
}
