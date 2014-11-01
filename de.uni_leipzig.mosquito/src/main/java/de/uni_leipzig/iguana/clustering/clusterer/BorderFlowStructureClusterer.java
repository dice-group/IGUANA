package de.uni_leipzig.mosquito.clustering.clusterer;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import de.uni_leipzig.mosquito.clustering.LogCluster;
import de.uni_leipzig.mosquito.clustering.LogSolution;
import de.uni_leipzig.mosquito.utils.EmailHandler;

/**
 * The Class BorderFlowStructureClusterer.
 * 
 * uses the border flow algorithm for clustering but instead of queries it clusters on their strucutres
 * 
 * @author Felix Conrads
 */
public class BorderFlowStructureClusterer implements Clusterer {

	/** The path. */
	public static String PATH = "cluster" + File.separator;
	
	/** The log. */
	private static Logger log = LogSolution.getLogger();
	
	/** The threshold queries. */
	private Integer thresholdQueries=10;
	
	/** The delta. */
	private Integer delta=2;
	
	/** The harden. */
	private String harden=null;
	
	/** The quality. */
	private String quality=null;
	
	/** The threshold. */
	private Double threshold=0.8;
	
	/** The caching. */
	private Boolean testOne=true, heuristic=true, caching=true;
	
	/** The threshold structs. */
	private Integer thresholdStructs;
	
	/** The min nodes. */
	private Integer minNodes;


	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.clustering.clusterer.Clusterer#cluster(java.lang.String, java.lang.String)
	 */
	@Override
	public void cluster(String logsPath, String queries) throws IOException {
		String start = DateFormat.getDateTimeInstance().format(new Date());
		Calendar calS = Calendar.getInstance();
		log.info("Starting ClusterProcess " + start);
		new File(PATH).mkdir();
		
		String queriesFile = PATH + "queryset.log";
		String structs = PATH +"structs.log";
		String freqFile = PATH + "freq.log";
		String sortedFreqFile = PATH + "sortedFreq.log";
		String freqFileQ = PATH + "freqQ.log";
		String sortedFreqFileQ = PATH + "sortedFreqQ.log";
		String clusterOutput = PATH +"cluster.log";
//		String clQueryOutput = PATH +"choosenClusterQuery.log";
		String simFile = PATH + "similarity.log";
		String queriesStruct = PATH + "choosenStructs.log";
		
		log.info("Start logs2Queries: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		LogSolution.logsToQueries(logsPath, queriesFile);
		log.info("End logs2Queries: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		
		log.info("Start queries2Frequents: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		// Structure work
		LogSolution.patternsToFrequents(queriesFile, freqFileQ, thresholdQueries);
		LogSolution.sortFrequents(freqFileQ, sortedFreqFileQ);
		log.info("End queries2Frequent: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		
		log.info("Start queries2Structs");
		LogSolution.queriesToStructure(queriesFile, structs);
		log.info("End queries2Structs"
			+ DateFormat.getDateTimeInstance().format(new Date()));
		
		log.info("Start structs2Frequents: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		// Structure work
		LogSolution.patternsToFrequents(structs, freqFile, thresholdStructs);
		LogSolution.sortFrequents(freqFile, sortedFreqFile);
		log.info("End structs2Frequent: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		
		log.info("Start calculating Structure similarities: ");
		LogSolution.similarity(sortedFreqFile, simFile, delta);
		log.info("End calculating Structure similarities: "+ DateFormat.getDateTimeInstance().format(new Date()));
		
		
		//Match Structs to most freq. queries in queriesStruct
		log.info("Start matching structs to queries: ");
		LogSolution.structsToFreqQueries(sortedFreqFile, sortedFreqFileQ, queriesStruct);
		log.info("End matching structs to queries: "+ DateFormat.getDateTimeInstance().format(new Date()));
		
		log.info("Start Clustering...");
		LogCluster.borderFlow(harden, quality, threshold, testOne, heuristic, caching, minNodes, queriesStruct, simFile, clusterOutput, queries);
		String end = DateFormat.getDateTimeInstance().format(new Date());
		Calendar calE = Calendar.getInstance();
		log.info("Ended ClusterProcess " + end);
		log.info("Needed Time: "
				+ EmailHandler.getWellFormatDateDiff(calS, calE));
		
		
		
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.clustering.clusterer.Clusterer#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties p) {
		if(p.get("threshold-queries")!=null){
			thresholdQueries=Integer.parseInt(String.valueOf(p.get("threshold-queries")).trim());
		}
		else{
			thresholdQueries=10;
		}
		if(p.get("threshold-structs")!=null){
			thresholdStructs=Integer.parseInt(String.valueOf(p.get("threshold-structs")).trim());
		}
		else{
			thresholdStructs=10;
		}
		if(p.get("delta")!=null){
			delta=Integer.parseInt(String.valueOf(p.get("delta")).trim());
		}
		else{
			delta=2;
		}
		if(p.get("min-nodes")!=null){
			minNodes=Integer.parseInt(String.valueOf(p.get("min-nodes")).trim());
		}
		else{
			minNodes=3;
		}
		if(p.get("conn-threshold")==null){
			threshold=0.8;
		}
		else{
			threshold=Double.valueOf(String.valueOf(p.get("conn-threshold")).trim());
		}
		if(p.get("test-one")!=null){
			testOne=Boolean.valueOf(String.valueOf(p.get("test-one")).trim());
		}
		else{
			testOne=true;
		}
		if(p.get("heuristic")!=null){
			heuristic=Boolean.valueOf(String.valueOf(p.get("heuristic")).trim());
		}else{
			heuristic=true;
		}
		if(p.get("caching")==null){
			caching=true;
		}
		else{
			caching=Boolean.valueOf(String.valueOf(p.get("caching")).trim());
		}
		harden=String.valueOf(p.get("harden")).trim();
		quality=String.valueOf(p.get("quality")).trim();
		
	}
	

}
