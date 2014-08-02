package de.uni_leipzig.mosquito.query;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.connection.ConnectionFactory;
import org.bio_gene.wookie.utils.GraphHandler;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;

import de.uni_leipzig.mosquito.data.TripleStoreHandler;
import de.uni_leipzig.mosquito.utils.RandomStringBuilder;

public class QueryHandler {
	
		
	public static void main(String args[]) throws IOException{
		String insert = "INSERT DATA {GRAPH %%r%% {%%r1%% %%r2%% %%d%%} {%%r1%% %%r3%% %%d%%}{%%r1%% %%r2%% %%r4%%} {%%r4%% %%r2%% %%i%%} {%%r4%% %%r5%% %%s%%}}";
		String select = "select distinct ?s where {?s <http://dbpedia.org/property/einwohner> ?v} LIMIT 10";
		String construct = "CONSTRUCT   { [] ?p ?name } WHERE { %%v1%% ?p ?name }";
		String ask = "PREFIX foaf:<http://xmlns.com/foaf/0.1/ASD>  ASK  { ?x foaf:name  %%v%% }";
		String describe = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> DESCRIBE %%v%%";
		PrintWriter pw = new PrintWriter(new File("queries.txt"));
		pw.write(ask);
		pw.println();
		pw.write(construct);
		pw.println();
		pw.write(select);
		pw.println();
		pw.write(describe);
		pw.println();
		pw.write(insert);
		pw.close();
		
		Connection con = ConnectionFactory.createImplConnection("dbpedia.org/sparql");
		QueryHandler qh = new QueryHandler(con, "queries.txt");
		qh.init();
	
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
	
	public static String ntToQuery(String file){
		return ntToQuery(new File(file));
	}
	
	public static String ntToQuery(File file){
		try{
			String query = "INSERT DATA {";
			FileInputStream fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			String line ="";
			while((line=br.readLine()) != null){
				if(!line.isEmpty() && !line.equals("\n")){
					continue;
				}
				query +="{";
				query +=line;
				query +="}";
			}
			br.close();
			return query;
		}
		catch(IOException e){
			return null;
		}
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
			if((p.toLowerCase().contains("ask") || p.toLowerCase().contains("select") || p.toLowerCase().contains("construct") || p.toLowerCase().contains("describe"))){
				//SELECT, ASK, DESCRIBE, CONSTRUCT
//				QueryFactory.create(p);
				valuesToCSV(p, String.valueOf(i));
			}
			else{
				//UPDATE 
				updatePattern(p, String.valueOf(i));
			}
			
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
			PrintWriter pw = new PrintWriter(f);
			String q = selectPattern(query);
			ResultSet res = con.execute(q);
			Boolean result= false;
			while(res.next()){
				result=true;
				ResultSetMetaData rsmd = res.getMetaData();
				int columns = rsmd.getColumnCount();
				String line ="";
				List<Object> vars = new LinkedList<Object>();
				for(int i=1 ;i<=columns;i++ ){
					Object current = res.getObject(i);
					Node cur = TripleStoreHandler.implToNode(current);
					vars.add(GraphHandler.NodeToSPARQLString(cur));
				}
				line = patternToQuery(pattern, vars);
				pw.write(line);
				pw.println();
				ret++;
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
		Pattern regex = Pattern.compile("%%v[0-9]*%%");
		Matcher matcher = regex.matcher(pattern);
		int i=0;
		List<String> replaced = new LinkedList<String>();
		while(matcher.find()){
			String var = matcher.group();
			if(!replaced.contains(var)){
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
		PrintWriter pw = new PrintWriter(f);
		for(int i=0;i<limit;i++){
			Pattern regex = Pattern.compile("%%i[0-9]*%%");
			Matcher matcher = regex.matcher(pattern);
			while(matcher.find()){
				query = query.replace(matcher.group(), String.valueOf(rand.nextInt()));
			}
			regex = Pattern.compile("%%d[0-9]*%%");
			matcher = regex.matcher(pattern);
			while(matcher.find()){
				query = query.replace(matcher.group(), String.valueOf(rand.nextDouble()));
			}
			regex = Pattern.compile("%%b[0-9]*%%");
			matcher = regex.matcher(pattern);
			while(matcher.find()){
				query = query.replace(matcher.group(), String.valueOf(rand.nextBoolean()));
		
			}
			regex = Pattern.compile("%%s[0-9]*%%");
			matcher = regex.matcher(pattern);
			while(matcher.find()){
				query = query.replace(matcher.group(), "'"+rsb.buildString(15)+"'");
		
			}
			regex = Pattern.compile("%%r[0-9]*%%");
			matcher = regex.matcher(pattern);
			while(matcher.find()){
				query = query.replace(matcher.group(), "<http://example.com/"+rsb.buildString(15)+">");
		
			}
			pw.write(query);
			pw.println();
			query = String.valueOf(pattern);
		}
		pw.close();
		
		return query;
	}
	
	private String selectPattern(String query){
		Pattern regex = Pattern.compile("%%v[0-9]*%%");
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
