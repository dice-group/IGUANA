package de.uni_leipzig.mosquito.clustering;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bio_gene.wookie.utils.LogHandler;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.lang.SPARQLParser;
import de.uni_leipzig.mosquito.query.QuerySorter;
import de.uni_leipzig.mosquito.utils.FileHandler;
import de.uni_leipzig.mosquito.utils.StringHandler;
import de.uni_leipzig.mosquito.utils.comparator.OccurrencesComparator;

/**
 * Provides some necessary algorithms for the clustering process
 * 
 * @author Felix Conrads
 */
public class LogSolution {
	
	/** The logger. */
	private static Logger log;
	
	static {
		log = Logger.getLogger(LogSolution.class.getName());
		LogHandler.initLogFileHandler(log, "LogCluster");
	}
	
	/**
	 * Gets the logger.
	 *
	 * @return the logger
	 */
	public static Logger getLogger(){
		return log;
	}
	

	/**
	 * Writes queries to their structures.
	 *
	 * @param input the name of the file with the queries to be converted
	 * @param output the name of the file in which the structures should be written
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void queriesToStructure(String input, String output) throws IOException{
		queriesToStructure(new File(input), new File(output));
	}	
	
	/**
	 * Writes queries to their structures.
	 *
	 * @param input the file with the queries to be converted
	 * @param output the file in which the structures should be written
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void queriesToStructure(File input, File output) throws IOException{
		output.createNewFile();
		FileInputStream fis = null;
		BufferedReader br = null;
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8), true);
		String line="";
		try{
			fis = new FileInputStream(input);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			long i=0;
			while((line = br.readLine())!= null){
				i++;
				line = queryToStructure(line);
				if(i%100000==0){
					log.info("Converted "+i+" Queries to structures");
				}
				pw.println(line);
				pw.flush();
			}
			log.info("Finished converting "+i+" queries to structures");
			pw.close();
		}
		catch(IOException e){
			
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
	}
	
	/**
	 * Replaces all Literals of the form '''what ever is written in here''' to '''string'''
	 * for a given query
	 * 
	 * @param query the query 
	 * @return the query with the replacement
	 */
	private static String queryWith(String query){
		String regex =  "'''(.*[^'])'''(|^^\\S+|@\\S+)";
		String q = query,ret = query;
		q = q.replaceAll("<\\S+>", "<>");
		Pattern p  = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
		Matcher m = p.matcher(q);

		while(m.find()){

			String literal = m.group();

			int index = literal.indexOf("'''", 3)+3;
			if(literal.length()>index){
				if(literal.charAt(index+1)=='@'){
					index = literal.substring(index+1).indexOf(" ");
				}else if(literal.substring(index+1, index+3)=="^^"){
					index = literal.substring(index+1).indexOf(">")+1;
				}
			}
			literal =  literal.substring(0, index);
			q = q.replace(literal, " $''string''$ ");
			ret = ret.replace(literal, "'''string'''");
			m = p.matcher(q);
		}

		return ret;
	}
	
	/**
	 * Converts a query to its structure by replacing:
	 * '''what ever stands here''' to '''string'''
	 * "what ever stands here" to "string"
	 * 'what ever stands here' to 'string'
	 * what:ever to prefix:suffix
	 * <what ever> to \<r\>
	 * <> to blank
	 * true or false to Bool
	 * any number to Number
	 * any variable to ?var
	 * any tag to \@tag
	 * any type of a literal to ^^"type"
	 *  
	 *  
	 * @param query the query
	 * @return the structure of the query
	 */
	public static String queryToStructure(String query){
		return queryWith(query)
				.replaceAll("'[^']*'", "\'string\'")
				.replaceAll("\"[^\"]*\"", "\\\"string\\\"")
				.replaceAll("\\S+\\s*:", " prefix:")
				.replaceAll("prefix:\\s*\\S+", "prefix:suffix ")
				.replaceAll("<\\S+>", "<r>")
				.replace("<>", "blank")
				.replaceAll("(true|false)", "Bool")
				.replaceAll("[+-]?[0-9]+(\\.[0-9])?", "Number")
				.replaceAll("\\?\\w+", "\\?var")
				.replaceAll("@\\w+", "@tag")
				.replaceAll("^^\"\\S+\"", "^^\"type\"")
				.replaceAll("\\s*\\.\\s*", " . ")
				.replaceAll("\\s*;\\s*", " ; ");
	}
	
	/**
	 * Converts the encoded queries in the logfiles to queries
	 *
	 * @param inputPath the path of the logfiles
	 * @param output the name of the output file in which the queries should be saved
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void logsToQueries(String inputPath, String output) throws IOException{
		logsToQueries(inputPath, new File(output));
	}
	
	/**
	 * Converts the encoded queries in the logfiles to queries
	 *
	 * @param inputPath the path of the logfiles
	 * @param output the output file in which the queries should be saved
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void logsToQueries(String inputPath, File output) throws IOException{
		output.createNewFile();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8), true);
		String[] extensions = {"log"};
		for(File f : FileHandler.getFilesInDir(inputPath, extensions)){
			logToQueries(pw, f);
		}
		pw.close();
	}
	
	/**
	 * Converts the encoded queries in a given logfile to queries
	 *
	 * @param input the name of the logfile
	 * @param output the name of the output file in which the queries should be saved
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void logToQueries(String input, String output) throws IOException{
		logToQueries(new File(input), new File(output));
	}
	
	/**
	 * Converts the encoded queries in a given logfile to queries
	 *
	 * @param input the logfile
	 * @param output the output file in which the queries should be saved
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void logToQueries(File input, File output) throws IOException{
		output.createNewFile();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8), true);
		logToQueries(pw, input);
		pw.close();
	}
	
	/**
	 * Converts the encoded queries in a given logfile to queries
	 *
	 * @param pw the Printwriter to use to write the queries
	 * @param input the name of the logfile
	 */
	public static void logToQueries(PrintWriter pw, String input){
		logToQueries(pw, new File(input));
	}
	
	/**
	  * Converts the encoded queries in a given logfile to queries
	 *
	 * @param pw the Printwriter to use to write the queries
	 * @param input the logfile
	 */
	public static void logToQueries(PrintWriter pw, File input){
		FileInputStream fis = null;
		BufferedReader br = null;
		String line;
		try{
			fis = new FileInputStream(input);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!= null){
				int index = line.indexOf("\"");
				int lastIndex = line.indexOf("\"", index+1);
				line = line.substring(index, lastIndex);
				String graph = null;
				Pattern  p = Pattern.compile("\\?default-graph-uri=[^&]+&");
				Matcher m = p.matcher(line);
				if(m.find()){
					String complete = m.group();
					graph = complete.substring(complete.indexOf("=")+1, complete.lastIndexOf("&"));
					graph = URLDecoder.decode(graph, "UTF-8");
				}
				
				line = line.replaceFirst(".*query=", "");
				if((index=line.indexOf("&"))>=0)
					line = line.substring(0, index);
				line = URLDecoder.decode(line, "UTF-8");
				line = line.replaceAll("^\\s+", "");
				line = queryVarRename(line);
				try{
					if(!QuerySorter.isSPARQL(line)&&!QuerySorter.isSPARQLUpdate(line)){
						throw new QueryException();
					}
					if(graph!=null &&QuerySorter.isSPARQL(line)){
						SPARQLParser sp = SPARQLParser.createParser(Syntax.syntaxSPARQL_11);
						Query q =sp.parse(QueryFactory.create(),line);
						q.addGraphURI(graph);
						line = q.toString().replace("\n", " ");
					}
					
					pw.println(line.replace("\n", " ").replace("\r", " ").replaceAll("\\s+", " "));
				}
				catch(QueryException e){
//					log.info("Messed up Query in logfile: "+line+"\nException: "+e);
				}

			}
			pw.flush();
		}
		catch(IOException e){
			
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
	}
	
	/**
	 * Renames the variables of a query to ?var 
	 * or in the case of mutliple variables to ?var1,...,?varn
	 * where n is the no of distinct variables in the query
	 *
	 * @param query the query
	 * @return the query with the renamed variables
	 */
	public static String queryVarRename(String query){
		String ret = query;
		Pattern p = Pattern.compile("\\?\\w+", Pattern.UNICODE_CHARACTER_CLASS);
		Matcher m = p.matcher(ret);
		String var="?var";
		int i=0;
		while(m.find()){
			ret = ret.replace(m.group(), var+i);
//			m = p.matcher(ret);
			i++;
		}
		return ret;
	}
	
	/**
	 * For all queries in the input file the query and its frequency
	 * will be written in the output file if the frequency is greater or equal than the threshold
	 *
	 * @param input the input file name
	 * @param output the output file name
	 * @param threshold the threshold 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void patternsToFrequents(String input, String output, Integer threshold) throws IOException{
		patternsToFrequents(new File(input), new File(output), threshold);
	}
	
	/**
	 * For all queries in the input file the query and its frequency will be saved in 
	 * a Map {query: frequency}
	 * 	
	 * @param input the input file name
	 * @return The map with the queries and their frequencies
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static Map<String, Integer> patternsToFrequents(String input) throws IOException{
		return patternsToFrequents(new File(input));
	}
	
	
	
	/**
	 * For all queries in the input file the query and its frequency will be saved in 
	 * a Map {query: frequency}
	 * 	
	 * @param input the input file
	 * @return The map with the queries and their frequencies
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static Map<String, Integer> patternsToFrequents(File input) throws IOException{
		
		Map<String, Integer> map = new HashMap<String, Integer>();
		//WEIRD STUFF HAPPENS IF THIS CODE IS NOT THERE oO
		map.put("test", 1);
		map.remove("test");
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		try{
			fis = new FileInputStream(input);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!= null){
				int j=0;
				if(line.isEmpty())continue;
				if(map.containsKey(line)){
					j = map.get(line);
				}
				map.put(line, j+1);
			}
		}
		catch(IOException e){
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		return map;
	}
	
	/**
	 * For all queries in the input file the query and its frequency
	 * will be written in the output file if the frequency is greater or equal than the threshold
	 *
	 * @param input the input file
	 * @param output the output file
	 * @param threshold the threshold 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void patternsToFrequents(File input, File output, Integer threshold) throws IOException{
		Map<String, Integer> map = patternsToFrequents(input);
		output.createNewFile();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8), true);
		for(String key : map.keySet()){
			if(map.get(key)>= threshold)
				pw.println(key+"\t"+map.get(key));
		}
		pw.close();
	}
	
	/**
	 * Sort the frequent queries and write the sorted queries into the output file.
	 *
	 * @param input the input file name
	 * @param output the output file name
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void sortFrequents(String input, String output) throws IOException{
		File f = new File(output);
		f.createNewFile();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8), true);
		for(String str : sortFrequents(new File(input))){
			pw.println(str);
		}
		pw.close();
	}
	
	/**
	 * Sort the frequent queries and returns the list where the queries are sorted by their frequency
	 *
	 * @param freq The name of the file with the frequent queries
	 * @return the sorted frequent list
	 */
	public static List<String> sortFrequents(String freq){
		return sortFrequents(new File(freq));
	}
	
	/**
	 * Sort the frequent queries and returns the list where the queries are sorted by their frequency
	 *
	 * @param freq The file with the frequent queries
	 * @return the sorted frequent list
	 */
	public static List<String> sortFrequents(File freq){
			List<String> sortedSet = new ArrayList<String>();
			FileInputStream fis = null;
			BufferedReader br = null;
			String line="";
			try{
				fis = new FileInputStream(freq);
				br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
				while((line = br.readLine())!= null){
					sortedSet.add(line);
				}
				Comparator<String> cmp = new OccurrencesComparator();
				Collections.sort(sortedSet, cmp);
			}
			catch(IOException e){
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
			finally{
				try {
					fis.close();
					br.close();
				} catch (IOException e) {
					
					LogHandler.writeStackTrace(log, e, Level.SEVERE);
				}
			}
			return sortedSet;
	}

	
	/**
	 * Calculates for every query with every query in the input file their similarity 
	 * and if the similarity is over a calculated threshold it will be written in the output file
	 *
	 * @param input the name of the input file with the queries
	 * @param output the name of the output file in  which the (calculated) query ids and their similarity will be written
	 * @param delta a given parameter for the threshold calculation <b>see also</b> <a href="http://www.aaai.org/ocs/index.php/AAAI/AAAI12/paper/download/5168/5384">DBpedia SPARQL Benchmark</a>

	 * @throws FileNotFoundException the file not found exception
	 */
	public static void similarity(String input, String output, int delta) throws FileNotFoundException{
		similarity(new File(input), new File(output), delta);
	}
	
	/**
	 * Calculates for every query with every query in the input file their similarity 
	 * and if the similarity is over a calculated threshold it will be written in the output file
	 *
	 * @param input the input file with the queries
	 * @param output the output file in  which the (calculated) query ids and their similarity will be written
	 * @param delta a given parameter for the threshold calculation <b>see also</b> <a href="http://www.aaai.org/ocs/index.php/AAAI/AAAI12/paper/download/5168/5384">DBpedia SPARQL Benchmark</a>
	 * @throws FileNotFoundException the file not found exception
	 */
	public static void similarity(File input, File output, int delta) throws FileNotFoundException{
		FileInputStream fis = null;
		FileInputStream fis2 = null;
		BufferedReader br1 = null;
		BufferedReader br2 = null;
		PrintWriter pw = new PrintWriter(output);
		int[] threshold = getThreshold(input, delta);
		log.info("Similarity Threshold vector: "+threshold[0]+"\nSimilarity Threshold levenshtein: "+threshold[1]);
		String line1, line2, line;
		try{
			fis = new FileInputStream(input);
			br1 = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			int n=0, o=0;
			while((line1 = br1.readLine())!= null){
				if(line1.isEmpty()){continue;}
				n++;
				fis2 = new FileInputStream(input);
				br2 = new BufferedReader(new InputStreamReader(fis2, Charset.forName("UTF-8")));
				o=0;
				while((line2 = br2.readLine())!= null){
					if(line2.isEmpty()){continue;}
					o++;
					if(o<=n){continue;}
					line =StringHandler.removeKeywordsFromQuery(line1.substring(0, line1.lastIndexOf("\t")));
					line2 =StringHandler.removeKeywordsFromQuery(line2.substring(0, line2.lastIndexOf("\t")));
					Double sim = getSimilarity(line, line2, threshold);
					if(sim ==null){continue;}
					pw.println("q"+n+"\t"+"q"+o+"\t"+sim);
				}
				fis2.close();
				log.info("Compared Query "+n+" with each other Query");
			}
			pw.close();
		}
		catch(IOException e){
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		finally{
			try {
				fis.close();
				fis2.close();
				br1.close();
				br2.close();
			} catch (IOException e) {
				
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
	}
	
	/**
	 * Gets the similarity of two queries.
	 * 
	 * <b>First step</b> calculating the feature vector distance and test if it's over the given threshold
 	 * <b>Second step</b> if this is the case, calculate the levenshtein distance with the given threshold
 	 * <b>Third step</b> if levenshtein is not 0.0 it will return the max of the two similarities
 	 *
	 * @param query1 the query1
	 * @param query2 the query2
	 * @param threshold the thresholds [featureVectorDistance, levenshtein steps]
	 * @return the similarity if the similarity is in the given thresholds, null otherwise
	 */
	public static Double getSimilarity(String query1, String query2, int[] threshold){
		double vdist = getFeatureVectorDistance(query1, query2);
		if(vdist>threshold[0]){
			return null;
		}
		vdist =1.0/(1+vdist);
//		ldist = lev.getSimilarity(query1, query2);
		double ldist = StringHandler.levenshtein(query1, query2, threshold[1]);
		if(ldist==0.0){
			return null;
		}
		return Math.max(vdist, ldist);
	}
	
	/**
	 * calculates the thresholds.
	 *
	 * @param input the name of the input file
	 * @param delta the delta
	 * @return the thresholds
	 */
	public static int[] getThreshold(String input, int delta){
		return getThreshold(new File(input), delta);
	}
	
	/**
	 * calculates the thresholds.
	 *
	 * @param input the input file
	 * @param delta the delta
	 * @return the thresholds
	 */
	public static int[] getThreshold(File input, int delta){
		int s=0;
		int[] ret = {0,0}; 
		int queryCount=0, featureCount=getFeatures().length;
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		try{
			fis = new FileInputStream(input);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!= null){
				if(line.isEmpty()){continue;}
				queryCount++;
				line =	line.substring(0, line.lastIndexOf("\t")).trim();
				s+= line.length();
			}
		}
		catch(IOException e){
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		
		ret[0]= Double.valueOf(Math.ceil((featureCount*queryCount)*delta/(100.0*queryCount))).intValue();
		log.info("Math.ceil(s*delta/(100.0*queryCount)): "+Math.ceil(s*delta/(100.0*queryCount)));
		log.info("queryCount: "+queryCount);
		log.info("s: "+s);
		ret[1]= Double.valueOf(Math.ceil(s*delta/(100.0*queryCount))).intValue();
		return ret;
	}	
	
	/**
	 * Gets the features.
	 *
	 * @return the features
	 */
	public static String[] getFeatures(){
		return new String []{"offset", "limit", "union", "optional", "filter", "regex", "sameterm",
			     "isliteral", "bound", "isiri", "isblank", "lang", "datatype", "distinct", "group", "order", "str"};

	}
	
	/**
	 * Counts triples in a given query.
	 *
	 * @param query the query
	 * @return the no of triples in the query
	 */
	public static int countTriplesInQuery(String query){
		int ret=0;
		String qOp = queryToStructure(query).replaceAll("[^\\{\\}\\.;]", "").replace(" ", "");
		String[] regexes = {"\\{\\}", ";","\\."};
		for(int i=0; i<regexes.length;i++){
			Pattern p = Pattern.compile(regexes[i], Pattern.UNICODE_CHARACTER_CLASS);
			Matcher m = p.matcher(qOp);
			while(m.find()){
				ret++;
			}
			qOp = qOp.replaceAll(regexes[i], "");

		}
		
//		log.info("Found "+ret+" triples in query");
		return ret;
	}
	
	/**
	 * Gets the sum of every query in the cluster by adding their frequents
	 *
	 * @param cluster the cluster
	 * @param freqQueries the name of the file with the frequent queries
	 * @return the frequent sum of the cluster
	 */
	public static Integer getFreqSum(String[] cluster, String freqQueries){
		return getFreqSum(cluster, new File(freqQueries));
	}
	
	/**
	 * Gets the sum of every query in the cluster by adding their frequents
	 *
	 * @param cluster the cluster
	 * @param freqQueries the file with the frequent queries
	 * @return the frequent sum of the cluster
	 */
	public static Integer getFreqSum(String[] cluster, File freqQueries){
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		int q=1;
		ArrayList<String> cl = new ArrayList<String>(Arrays.asList(cluster));
		Integer ret=0;
		try{
			fis = new FileInputStream(freqQueries);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!= null && !cl.isEmpty()){
				if(line.isEmpty()){continue;}
				for(String qID : cl){
					if(qID.equals("q"+q)){
						ret+=Integer.parseInt(line.substring(line.lastIndexOf("\t")+1));
						cl.remove(qID);
						break;
					}
				}
				q++;
			}
		}
		catch(IOException e){
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		return ret;
	}
	
	
	/**
	 * Matches the structures in a given file with the most frequent query  and write it 
	 * in the output file
	 * 
	 * @param structs the name of the file with the structures to match with
	 * @param freq the name of the file with the sorted frequent queries
	 * @param output the name of the output file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void structsToFreqQueries(String structs, String freq, String output) throws IOException{
		structsToFreqQueries(new File(structs), new File(freq), new File(output));
	}

	/**
	 * Matches the structures in a given file with the most frequent query  and write it 
	 * in the output file
	 * 
	 * @param structs the file with the structures to match with
	 * @param freq the file with the sorted frequent queries
	 * @param output the output file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void structsToFreqQueries(File structs, File freq, File output) throws IOException{
		output.createNewFile();
		PrintWriter pw = new PrintWriter(output);
		FileInputStream fis = null;
		BufferedReader br = null;
		FileInputStream fis2 = null;
		BufferedReader br2 = null;
		String line, line2, struct;
		try{
			fis = new FileInputStream(structs);
			
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!= null){
				fis2 = new FileInputStream(freq);
				br2 = new BufferedReader(new InputStreamReader(fis2, Charset.forName("UTF-8")));
				line = line.substring(0, line.lastIndexOf("\t"));
				while((line2=br2.readLine())!=null){
					struct = queryToStructure(line2.substring(0, line2.lastIndexOf("\t")));
					if(struct.equals(line)){
						pw.println(line2);
						break;
					}
				}
				br2.close();
//				pw.flush();
			}
		}
		catch(IOException e){
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		pw.close();
	}
	
	
	/**
	 * Gets the feature vector.
	 *
	 * @param query the query
	 * @param features the features
	 * @return the feature vector
	 */
	private static Byte[] getFeatureVector(String query, String[] features){
		Byte[] vec = new Byte[features.length];
		for(int i=0;i<features.length;i++){
			if(query.contains(features[i])){
				vec[i]=1;
			}
			else{
				vec[i]=0;
			}
		}
		return vec;
	}
	
	/**
	 * Gets the feature vector distance.
	 *
	 * @param query1 the query1
	 * @param query2 the query2
	 * @return the feature vector distance
	 */
	private static double getFeatureVectorDistance(String query1, String query2){
		Byte[] q1= getFeatureVector(query1, getFeatures());
		Byte[] q2= getFeatureVector(query2, getFeatures());
		double ret =0;
		for(int i=0;i<q1.length;i++){
			ret+=Math.pow(q1[i]-q2[i], 2);
		}
		return Math.abs(ret);
	}
	
	
	
	

}
