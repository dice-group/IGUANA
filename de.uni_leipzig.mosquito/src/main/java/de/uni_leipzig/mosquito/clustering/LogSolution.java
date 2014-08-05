package de.uni_leipzig.mosquito.clustering;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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

import de.uni_leipzig.mosquito.utils.FileHandler;
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
		String test = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX dbpedia2: <http://dbpedia.org/property/> PREFIX owl: <http://dbpedia.org/ontology/> SELECT ?yearVar WHERE { ?subject dbpedia2:name ?name . ?subject dbpedia2:artist ?artist . ?artist dbpedia2:name \"ÊÈÍÎ\"@en . ?subject rdf:type <http://dbpedia.org/ontology/Album> . ?subject owl:releaseDate ?yearVar.FILTER (regex(str(?name), \"Ëó÷øèå ïåñíè 88-90\"@en, \"i\"))}Limit 10";
		System.out.println(queryToStructure(test));
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
				.replaceAll("[0-9]+(\\.[0-9])?", "Number")
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

				pw.println(line.replace("\n", " ").replace("\r", " ").replaceAll("\\s+", " "));

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

	
	public static int totalFreq(List<String> cluster){
		int i=0;
		for(String line : cluster)
			i += Integer.parseInt(line.substring(line.lastIndexOf('\t'+1)));
		return i;
	}
	
	
}
