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
import de.uni_leipzig.bf.cluster.harden.HardenSharedShed;
import de.uni_leipzig.bf.cluster.harden.HardenSuperset;
import de.uni_leipzig.bf.cluster.harden.QualityMeasureRelativeFlow;
import de.uni_leipzig.bf.cluster.harden.QualityMeasureSilhouette;
import de.uni_leipzig.mosquito.query.PatternSolution;


// TODO: Auto-generated private Javadoc
/**
 * Provides the clustering algorithms for the log clustering process
 * 
 * @author Felix Conrads
 * 
 */
public class LogCluster {
	
	/** The logger. */
	private static Logger log = LogSolution.getLogger();


	/**
	 * the sorted structure algoritm.
	 * for every frequent structure (cluster) it will match the most frequent query (if there is one) 
	 * and write it in the output file
	 *
	 * @param inputQueries the name of the file with the sorted frequent queries
	 * @param inputSortedStructure the name of the file with the sorted frequent structures
	 * @param output the name of the file in which the resulting queries should be saved
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void sortedStructure(String inputQueries,
			String inputSortedStructure, String output) throws IOException {
		sortedStructure(new File(inputQueries), new File(inputSortedStructure),
				new File(output));
	}

	/**
	 * the sorted structure algoritm.
	 * for every frequent structure (cluster) it will match the most frequent query (if there is one) 
	 * and write it in the output file
	 *
	 * @param inputQueries the file with the sorted frequent queries
	 * @param inputSortedStructure the file with the sorted frequeunt structures
	 * @param output the file in which the resulting queries should be saved
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
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
				String testQuery = PatternSolution.queryToPattern(cluster.get(0));
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

	/**
	 * The border flow clustering algorithm. 
	 * for more information on the border flow clusterer  @see <a href="http://borderflow.sourceforge.net/">Border Flow</a>
	 * 
	 * <b>First step:</b> processing the border flow clustering algorithm
	 * <b>Second step:</b> with the resulting cluster rank those clusters and for every feature 
	 * tries to get a query by the best ranked cluster
	 * 
	 *
	 * @param clusterHarden <b>see</b> <a href="http://borderflow.sourceforge.net/">Border Flow</a>
	 * @param qualityMeasure <b>see</b> <a href="http://borderflow.sourceforge.net/">Border Flow</a>
	 * @param connThreshold <b>see</b> <a href="http://borderflow.sourceforge.net/">Border Flow</a>
	 * @param testOne <b>see</b> <a href="http://borderflow.sourceforge.net/">Border Flow</a>
	 * @param heuristic <b>see</b> <a href="http://borderflow.sourceforge.net/">Border Flow</a>
	 * @param caching <b>see</b> <a href="http://borderflow.sourceforge.net/">Border Flow</a>
	 * @param minNodes the no of minimal Nodes a cluster should have
	 * @param inputQueries the name of the file with the frequent queries
	 * @param input the name of the file with the queries IDs and their similarity 
	 * @param clusterOutput the name of the output file in which the cluster should be written
	 * @param output the name of the output file in which the resulting queries should be written
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void borderFlow(String clusterHarden, String qualityMeasure, double connThreshold, boolean testOne, boolean heuristic, boolean caching, Integer minNodes, String inputQueries, String input, String clusterOutput, String output) throws IOException{
		Harden h = null;
		switch(clusterHarden.toLowerCase().replaceAll("\\s", "")){
		case"hardensharedshed":
			switch(qualityMeasure.toLowerCase().replaceAll("\\s", "")){
			case "qualitymeasurerelativeflow":
				h = new HardenSharedShed(new QualityMeasureRelativeFlow());
				break;
			case "qualitymeasuresilhoutte":
				h = new HardenSharedShed(new QualityMeasureSilhouette());
				break;
			default:
				h = new HardenSharedShed();
			}
			break;
		case"hardensuperset":h = new HardenSuperset();break;
		default:
			h = new HardenSuperset();
		}
		BorderFlow bf = new BorderFlow(input, h);
		bf.clusterToFile(clusterOutput, connThreshold, testOne, heuristic, caching);
//		queryListToFile(bfClusterToQuerySet(input, clusterOutput), inputQueries, clQueryOutput);
		rankAndChoose(inputQueries, clusterOutput, output, minNodes);
	}
	
	/**
	 * Query list to file.
	 *
	 * @param queryList the query list
	 * @param input the input
	 * @param output the output
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("unused")
	private static void queryListToFile(LinkedList<Integer> queryList, String input, String output) throws IOException{
		queryListToFile(queryList, new File(input), new File(output));
	}
	
	/**
	 * Query list to file.
	 *
	 * @param queryList the query list
	 * @param input the input
	 * @param output the output
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
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
					pw.println(PatternSolution.queryToPattern(line));
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
	
	

	
	/**
	 * Query id list to queries.
	 *
	 * @param queryList the query list
	 * @param input the input
	 * @return the string[]
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static String[] queryIDListToQueries(LinkedList<Integer> queryList, File input) throws IOException{
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		String[] ret = new String[queryList.size()];
		Collections.sort(queryList);
//		Collections.reverse(queryList);
		try{
			fis = new FileInputStream(input);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			int i=0, j=0, t=0;
			
			while((line = br.readLine())!= null){
				j=queryList.get(t);
				if(j==i){
					line = line.substring(0, line.lastIndexOf("\t"));
					ret[t] = PatternSolution.queryToPattern(line);
					t++;
					if(t==queryList.size()){
						break;
					}
				}
				i++;
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
	 * Bf cluster to query set.
	 *
	 * @param inputSim the input sim
	 * @param input the input
	 * @param minNodes the min nodes
	 * @return the linked list
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("unused")
	private static LinkedList<Integer> bfClusterToQuerySet(String inputSim, String input, Integer minNodes) throws IOException{
		return bfClusterToQuerySet(inputSim, new File(input), minNodes);
	}

	/**
	 * Bf cluster to query set.
	 *
	 * @param inputSim the input sim
	 * @param input the input
	 * @param minNodes the min nodes
	 * @return the linked list
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static LinkedList<Integer> bfClusterToQuerySet(String inputSim, File input, Integer minNodes) throws IOException {
//			output.createNewFile();
			FileInputStream fis = null;
			FileInputStream fis2 = null;
			BufferedReader br = null;
			BufferedReader br2 = null;
			String line="", line2;
			LinkedList<Integer> queryList = new LinkedList<Integer>();

			try{
				fis = new FileInputStream(input);
				fis2 = new FileInputStream(input);
				br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
				br2 = new BufferedReader(new InputStreamReader(fis2, Charset.forName("UTF-8")));
				br.readLine(); //HEADER
				br2.readLine();
				while((line = br.readLine())!= null){
					String[] s = line.split("\t");
					String[] cluster = s[1].replaceAll("(\\[|\\])", "").split(",\\s*");
					while((line2 =br2.readLine())!=null){
						if(s[1].equals(line2.split("\t")[1])){
							break;
						}
					}
					if(cluster.length<minNodes){
						queryList.add(getQueryID(inputSim, cluster));
					}
					br2 = new BufferedReader(new InputStreamReader(fis2, Charset.forName("UTF-8")));

				}
			}
			catch(IOException e){
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
			finally{
				try {
					fis.close();
					fis2.close();
					br.close();
					br2.close();
				} catch (IOException e) {
					
					LogHandler.writeStackTrace(log, e, Level.SEVERE);
				}
			}
			return queryList;
	}
	
	/**
	 * Gets the query id.
	 *
	 * @param simFile the sim file
	 * @param cluster the cluster
	 * @return the query id
	 */
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
	
	/**
	 * Rank and choose.
	 *
	 * @param freqQueries the freq queries
	 * @param input the input
	 * @param output the output
	 * @param minNodes the min nodes
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static void rankAndChoose(String freqQueries, String input, String output, Integer minNodes) throws IOException{
		rankAndChoose(new File(freqQueries), new File(input), new File(output), minNodes);
	}
	
	/**
	 * Rank and choose.
	 *
	 * @param freqQueries the freq queries
	 * @param input the input
	 * @param output the output
	 * @param minNodes the min nodes
	 * @return the string[]
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static String[] rankAndChoose(File freqQueries,File input, File output, Integer minNodes) throws IOException{
		FileInputStream fis = null;
		BufferedReader br = null;
		output.createNewFile();
		PrintWriter pw = new PrintWriter(output);
		String line;
		String[] feat = LogSolution.getFeatures();
		Integer[] momFreq = new Integer[feat.length];
		for(int i=0; i<feat.length;i++){
			momFreq[i]=0;
		}
		String[] momQueries = new String[feat.length];

		try{
			fis = new FileInputStream(input);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!= null){
				if(line.isEmpty()){continue;}
				String[] cluster = line.split("\t")[1].replaceAll("(\\[|\\])", "").split(",\\s*");
				if(cluster.length<minNodes){
					continue;
				}
				Integer freq = LogSolution.getFreqSum(cluster, freqQueries);

				cluster = queryIDListToQueries(clusterToID(cluster), freqQueries);
				for(int i=0;i<feat.length;i++){
					if(momFreq[i]>=freq){
						continue;
					}
					for(int j=0; j<cluster.length;j++){
						try{
						if(cluster[j]==null){
							continue;
						}
						if(cluster[j].replaceAll("\\<.*?>","").replaceAll("\\\".*?\"","").contains(feat[i])){
							momFreq[i]=freq;
							momQueries[i]=cluster[j];
							break;
						}
						}catch(Exception e){
							e.printStackTrace();
						}
					}
				}
			}
			int k;
			for(k=0;k<momQueries.length-1;k++){
				if(momQueries[k]!=null){
					
					pw.println(momQueries[k]);
				}
				else{
					log.info("No Query for feature "+feat[k]);
				}
			}
			pw.print(momQueries[k]);
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
		return momQueries;
	}
	
	/**
	 * Cluster to id.
	 *
	 * @param cluster the cluster
	 * @return the linked list
	 */
	private static LinkedList<Integer> clusterToID(String[] cluster){
		LinkedList<Integer> ret = new LinkedList<Integer>();
		for(String cl : cluster){
			ret.add(Integer.parseInt(cl.substring(1)));
		}
		return ret;
	}
	
	
}
