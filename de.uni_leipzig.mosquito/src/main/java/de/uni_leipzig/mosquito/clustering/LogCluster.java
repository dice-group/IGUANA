package de.uni_leipzig.mosquito.clustering;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.utils.LogHandler;

import de.uni_leipzig.bf.cluster.BorderFlow;
import de.uni_leipzig.bf.cluster.harden.Harden;
import de.uni_leipzig.bf.cluster.harden.HardenSuperset;
import de.uni_leipzig.mosquito.query.PatternSolution;
import de.uni_leipzig.mosquito.query.QueryHandler;


public class LogCluster {
	
	private static Logger log = LogSolution.getLogger();


	public static void main(String[] argc){
//		try {
//			clusterProcess("../../LogFiles", "queriesFile.txt", 100, 100);
			
//		} catch (IOException e) {
//			
//			LogHandler.writeStackTrace(log, e, Level.SEVERE);
//		}
	}

	public static void sortedStructure(String inputQueries,
			String inputSortedStructure, String output) throws IOException {
		sortedStructure(new File(inputQueries), new File(inputSortedStructure),
				new File(output));
	}

	public static void sortedStructure(File inputQueries,
			File inputSortedStructure, File output) throws IOException {
		output.createNewFile();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8), true);
		FileInputStream fisq = null;
		FileInputStream fisiss = null;
		BufferedReader brq = null;
		BufferedReader briss = null;
		String line, query;
		try {
			fisiss = new FileInputStream(inputSortedStructure);
			briss = new BufferedReader(new InputStreamReader(fisiss,
					Charset.forName("UTF-8")));
			// For each cluster get Queries.
			while ((line = briss.readLine()) != null) {
				LinkedList<String> cluster = new LinkedList<String>();
				if(line.isEmpty())
					continue;
				
				fisq = new FileInputStream(inputQueries);
				brq = new BufferedReader(new InputStreamReader(fisq,
						Charset.forName("UTF-8")));
				while ((query = brq.readLine()) != null) {
					if(query.isEmpty())
						continue;
					// Only Query and Structure without Frequence
					String q = query.substring(0, query.lastIndexOf("\t"));
					String struct = line.substring(0, line.lastIndexOf("\t"));
					String struct2 = LogSolution.queryToStructure(q);
					if (struct2.equals(struct)) {
						// Query is in Cluster and as inputQuery is sorted by
						// Frequence
						// you can just add it.
						cluster.add(q);
					}
				}
				try{
					fisq.close();
					brq.close();
				}
				catch(IOException e){
					LogHandler.writeStackTrace(log, e, Level.SEVERE);
				}
				/*
				 * Now we have our Cluster. Here we must test if the queries
				 * have tuples. The first which has tuples will be the tested
				 * query for this cluster
				 */
				if(cluster.isEmpty()){
					
					log.info("Cluster "+line+" has no frequent queries");
					continue;
				}
				String testQuery = QueryHandler.queryIRIsToVars(PatternSolution.queryToPattern(cluster.get(0)));
				// write test Query into the final output File
				pw.println(testQuery);

			}
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		} finally {
			try {
				fisiss.close();
				briss.close();
				pw.close();
			} catch (IOException e) {
				
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		} 
	}

	public static void borderFlow(String inputQueries, String input, String clusterOutput, String clQueryOutput,String output) throws IOException{
		Harden h = new HardenSuperset();
		BorderFlow bf = new BorderFlow(input, h);
		//TODO
		bf.clusterToFile(clusterOutput, 0.8, false, true, true);
		queryListToFile(bfClusterToQuerySet(input, clusterOutput), inputQueries, clQueryOutput);
		
	}
	
	private static void queryListToFile(LinkedList<Integer> queryList, String input, String output) throws IOException{
		queryListToFile(queryList, new File(input), new File(output));
	}
	
	private static void queryListToFile(LinkedList<Integer> queryList, File input , File output) throws IOException{
		output.createNewFile();
		PrintWriter pw = new PrintWriter(output);
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		Collections.sort(queryList);
		Collections.reverse(queryList);
		try{
			fis = new FileInputStream(input);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			int i=0, j=0, t=0;
			j=queryList.get(t);
			while((line = br.readLine())!= null){
				if(j==i){
					line = line.substring(0, line.lastIndexOf("\t"));
					pw.println(QueryHandler.queryIRIsToVars(PatternSolution.queryToPattern(line)));
					j =queryList.get(++t);
				}
				i++;
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
	
	private static LinkedList<Integer> bfClusterToQuerySet(String inputSim, String input) throws IOException{
		return bfClusterToQuerySet(inputSim, new File(input));
	}

	private static LinkedList<Integer> bfClusterToQuerySet(String inputSim, File input) throws IOException {
//			output.createNewFile();
			FileInputStream fis = null;
			BufferedReader br = null;
			String line="";
			LinkedList<Integer> queryList = new LinkedList<Integer>();

			try{
				fis = new FileInputStream(input);
				br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
				br.readLine(); //HEADER
				while((line = br.readLine())!= null){
					String[] s = line.split("\t");
					String[] cluster = s[1].replaceAll("(\\[|\\])", "").split(",\\s*");
					queryList.add(getQueryID(inputSim, cluster));
					
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
			return queryList;
	}
	
	private static Integer getQueryID(String simFile, String[] cluster){
		Integer ret=null;
		
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		for(String str : cluster){
			map.put(Integer.parseInt(str.substring(1)), 0);
		}
		try{
			fis = new FileInputStream(simFile);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			br.readLine(); //HEADER
			while((line = br.readLine())!= null){
				String[] split = line.split("\t");
				if(map.containsKey(split[0])){
					map.put(Integer.parseInt(split[0].substring(1)), map.get(split)+1);
				}
				if(map.containsKey(split[2])){
					map.put(Integer.parseInt(split[1].substring(1)), map.get(split)+1);
				}
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
		int occurs=0;
		for(Integer key : map.keySet()){
			if(map.get(key)>occurs){
				ret = key;
			}
		}
		return ret;
	}

}
