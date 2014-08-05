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
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.utils.LogHandler;

import de.uni_leipzig.mosquito.query.PatternSolution;
import de.uni_leipzig.mosquito.utils.EmailHandler;


public class LogCluster {
	
	private static Logger log = LogSolution.getLogger();

	private static String PATH = "cluster" + File.separator;

	public static void main(String[] argc){
		try {
			clusterProcess("../../LogFiles", "queriesFile.txt", 100, 100);

		} catch (IOException e) {
			
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
	}
	
	public static void clusterProcess(String logsPath, String outputFile,
			Integer thresholdStructs, Integer thresholdQueries)
			throws IOException {
		clusterProcess(logsPath, new File(outputFile), thresholdStructs,
				thresholdQueries);
	}

	public static void clusterProcess(String logsPath, File file,
			Integer thresholdStructs, Integer thresholdQueries)
			throws IOException {
		String start = DateFormat.getDateTimeInstance().format(new Date());
		Calendar calS = Calendar.getInstance();
		log.info("Starting ClusterProcess "+start);
		new File(PATH).mkdir();
		String queriesFile = PATH + "queryset.log";
		String structFile = PATH + "structs.log";
		String freqStructFile = PATH + "freqStruct.log";
		String sortedFreqStructFile = PATH + "sortedFreqStruct.log";
		String freqFile = PATH + "freq.log";
		String sortedFreqFile = PATH + "sortedFreq.log";

		log.info("Start logs2Queries: "+DateFormat.getDateTimeInstance().format(new Date()));
		LogSolution.logsToPatterns(logsPath, queriesFile);
		log.info("End logs2Queries: "+DateFormat.getDateTimeInstance().format(new Date()));

		// Structure work
		log.info("Start queries2FrequentStructs: "+DateFormat.getDateTimeInstance().format(new Date()));
		log.info("Start queries2Structure...");
		LogSolution.queriesToStructure(queriesFile, structFile);
		log.info("Start structs2Frequents...");
		LogSolution.patternsToFrequents(structFile, freqStructFile,
				thresholdStructs);
		log.info("Start sorting structs...");
		LogSolution.sortFrequents(freqStructFile, sortedFreqStructFile);
		log.info("End queries2FrequentStructs: "+DateFormat.getDateTimeInstance().format(new Date()));

		log.info("Start queries2Frequents: "+DateFormat.getDateTimeInstance().format(new Date()));
		// Structure work
		LogSolution.patternsToFrequents(queriesFile, freqFile,
				thresholdQueries);
		LogSolution.sortFrequents(freqFile, sortedFreqFile);
		log.info("End queries2Frequent: "+DateFormat.getDateTimeInstance().format(new Date()));


		//Clustering
		log.info("Start Clustering: "+DateFormat.getDateTimeInstance().format(new Date()));
		sortedStructure(new File(sortedFreqFile), new File(sortedFreqStructFile), file);
		String end = DateFormat.getDateTimeInstance().format(new Date());
		Calendar calE = Calendar.getInstance();
		log.info("Ended ClusterProcess "+end);
		log.info("Needed Time: "+EmailHandler.getWellFormatDateDiff(calS, calE));
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
				// TODO! test if tuples available
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

}
