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
import java.io.RandomAccessFile;
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

import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryFactory;

import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;
import de.uni_leipzig.mosquito.utils.FileHandler;
import de.uni_leipzig.mosquito.utils.StringHandler;
import de.uni_leipzig.mosquito.utils.comparator.OccurrencesComparator;

public class LogSolution {
	
	private static Logger log;
	
	static {
		log = Logger.getLogger(LogSolution.class.getName());
		LogHandler.initLogFileHandler(log, "LogCluster");
	}
	
	public static Logger getLogger(){
		return log;
	}

	public static void main(String[] argc) throws IOException{
//		logToPatterns("./src/main/resources/logFile.log", "test.log");
//		queriesToStructure("test.log", "structure.log");
//		patternsToFrequents("structure.log", "frequent.log", 2);
//		sortFrequents("frequent.log", "sortedFrequent.log");
		String test = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX dbpedia2: <http://dbpedia.org/property/> PREFIX owl: <http://dbpedia.org/ontology/> SELECT ?yearVar WHERE { {?subject dbpedia2:name ?name } {?subject dbpedia2:artist ?artist . ?artist dbpedia2:name \"ÊÈÍÎ\"@en . ?subject rdf:type <http://dbpedia.org/ontology/Album> . ?subject owl:releaseDate ?yearVar.FILTER (regex(str(?name), \"Ëó÷øèå ïåñíè.. 88-90\"@en, \"i\"))}}Limit 10";
		log.info(queryToStructure(test));
		countTriplesInQuery(test);
		
		Levenshtein lev = new Levenshtein();
		log.info(String.valueOf(lev.getSimilarity("FELIS", "HELIX")));
	}
	

	public static void queriesToStructure(String input, String output) throws IOException{
		queriesToStructure(new File(input), new File(output));
	}	
	
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
	
	public static String queryToStructure(String query){
		return queryWith(query)
				.replaceAll("'[^']*'", "\'string\'")
				.replaceAll("\"[^\"]*\"", "\\\"string\\\"")
				.replaceAll("\\S+\\s*:", "prefix:")
				.replaceAll("prefix:\\s*\\S+", "prefix:suffix")
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
	
	public static void logsToPatterns(String inputPath, String output) throws IOException{
		logsToPatterns(inputPath, new File(output));
	}
	
	public static void logsToPatterns(String inputPath, File output) throws IOException{
		output.createNewFile();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8), true);
		String[] extensions = {"log"};
		for(File f : FileHandler.getFilesInDir(inputPath, extensions)){
			logToPatterns(pw, f);
		}
		pw.close();
	}
	
	public static void logToPatterns(String input, String output) throws IOException{
		logToPatterns(new File(input), new File(output));
	}
	
	public static void logToPatterns(File input, File output) throws IOException{
		output.createNewFile();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8), true);
		logToPatterns(pw, input);
		pw.close();
	}
	
	public static void logToPatterns(PrintWriter pw, String input){
		logToPatterns(pw, new File(input));
	}
	
	public static void logToPatterns(PrintWriter pw, File input){
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
				line = line.replaceFirst(".*query=", "");
				if((index=line.indexOf("&"))>=0)
					line = line.substring(0, index);
				line = URLDecoder.decode(line, "UTF-8");
				line = line.replaceAll("^\\s+", "");
				line = queryVarRename(line);
				try{
					QueryFactory.create(line);
					pw.println(line.replace("\n", " ").replace("\r", " ").replaceAll("\\s+", " "));
				}
				catch(QueryException e){
					log.info("Messed up Query in logfile: "+line+"\nException: "+e);
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
	
	public static void patternsToFrequents(String input, String output, Integer threshold) throws IOException{
		patternsToFrequents(new File(input), new File(output), threshold);
	}
	
	public static Map<String, Integer> patternsToFrequents(String input) throws IOException{
		return patternsToFrequents(new File(input));
	}
	
	public static void testPatternsToFrequents(String input, String output, Integer threshold) throws IOException{
		testPatternsToFrequents(new File(input), new File(output), threshold);
	}
	
	public static void testPatternsToFrequents(File input, File output, Integer threshold) throws IOException{
		FileInputStream fis = null;
		BufferedReader br = null;
		output.createNewFile();
		RandomAccessFile ra;
		String line="";
		try{
			fis = new FileInputStream(input);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!= null){
				if(line.isEmpty())continue;
				String line2;
				ra = new RandomAccessFile(output, "rw");
				while((line2 = ra.readLine())!=null){
					String q = line2.substring(0, line2.indexOf("\t"));
					Integer i = Integer.parseInt(line2.substring(line2.indexOf("\t")+1));
					if(line.equals(q))
						ra.writeBytes(q+"\t"+String.valueOf(i+1));
				}
				ra.close();
			}
		}
		catch(IOException e){
			
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		finally{
			try {
//				ra.close();
				fis.close();
				br.close();
			} catch (IOException e) {
				
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
	}
	
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
	
	public static void sortFrequents(String input, String output) throws IOException{
		File f = new File(output);
		f.createNewFile();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8), true);
		for(String str : sortFrequents(new File(input))){
			pw.println(str);
		}
		pw.close();
	}
	
	public static List<String> sortFrequents(String freq){
		return sortFrequents(new File(freq));
	}
	
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

	
	public static void similarity(String input, String output, int delta) throws FileNotFoundException{
		similarity(new File(input), new File(output), delta);
	}
	
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
	
	public static Double getSimilarity(String query1, String query2, int[] threshold){
		double vdist = getFeatureVectorDistance(query1, query2);
		if(vdist>threshold[0]){
			return null;
		}
		vdist =1.0/(1+vdist);
		
		
 
//		ldist = lev.getSimilarity(query1, query2);
		double ldist = StringHandler.levenshtein(query1, query2, threshold[1]);
//		if(Math.max(query1.length(), query2.length())*(1-ldist)>threshold[1]){
//			return null;
//		}
		return Math.max(vdist, ldist);
	}
	
	public static int[] getThreshold(String input, int delta){
		return getThreshold(new File(input), delta);
	}
	
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
	
	public static String[] getFeatures(){
		return new String []{"offset", "limit", "union", "optional", "filter", "regex", "sameterm",
			     "isliteral", "bound", "isiri", "isblank", "lang", "datatype", "distinct", "group", "order", "str"};

	}
	
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
	
	public static Integer getFreqSum(String[] cluster, String freqQueries){
		return getFreqSum(cluster, new File(freqQueries));
	}
	
	public static Integer getFreqSum(String[] cluster, File freqQueries){
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		int q=0;
		List<String> cl = Arrays.asList(cluster);
		Integer ret=0;
		try{
			fis = new FileInputStream(freqQueries);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!= null && cl.isEmpty()){
				if(line.isEmpty()){continue;}
				for(String qID : cl){
					if(qID.equals("q"+q)){
						ret+=Integer.parseInt(line.substring(line.lastIndexOf("\t")+1, line.length()));
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
	
	
	public static void structsToFreqQueries(String structs, String freq, String output) throws IOException{
		structsToFreqQueries(new File(structs), new File(freq), new File(output));
	}

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
			fis2 = new FileInputStream(freq);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			br2 = new BufferedReader(new InputStreamReader(fis2, Charset.forName("UTF-8")));
			while((line = br.readLine())!= null){
				line = line.substring(0, line.lastIndexOf("\t"));
				while((line2=br2.readLine())!=null){
					struct = queryToStructure(line2.substring(0, line2.lastIndexOf("\t")));
					if(struct.equals(line)){
						pw.println(line2);
					}
				}
				pw.flush();
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
	}
	
	
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
