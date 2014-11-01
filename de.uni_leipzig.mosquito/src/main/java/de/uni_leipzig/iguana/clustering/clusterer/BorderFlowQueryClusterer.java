package de.uni_leipzig.iguana.clustering.clusterer;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import de.uni_leipzig.iguana.clustering.LogCluster;
import de.uni_leipzig.iguana.clustering.LogSolution;
import de.uni_leipzig.iguana.utils.EmailHandler;

/**
 * The Class BorderFlowQueryClusterer.
 * 
 * uses the border flow algorithm for clustering the queries and returns query patterns
 * 
 * @author Felix Conrads
 */
public class BorderFlowQueryClusterer implements Clusterer {

	/** The path. */
	public static String PATH = "cluster" + File.separator;
	
	/** The log. */
	private static Logger log = LogSolution.getLogger();
	
	/** The threshold queries. */
	private Integer thresholdQueries=10;
	
	/** The delta. */
//	private Integer delta=2;
	
	/** The harden. */
	private String harden=null;
	
	
	/** The threshold. */
	private Double threshold=0.8;
	
	/** The caching. */
	private Boolean testOne=true, heuristic=true, caching=true;
	
	/** The min nodes. */
	private Integer minNodes;

	private Boolean onlyComplexQueries;
	
//	public static void main(String[] argc){
//		BorderFlowQueryClusterer bfqc = new BorderFlowQueryClusterer();
//		bfqc.setProperties(new Properties());
//		try {
//			bfqc.cluster("../../LogFiles", "Queries.txt");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	

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
		String freqFile = PATH + "freq.log";
		String sortedFreqFile = PATH + "sortedFreq.log";
		String clusterOutput = PATH +"cluster.log";
//		String clQueryOutput = PATH +"choosenClusterQuery.log";
		String simFile = PATH + "similarity.log";
		
		log.info("Start logs2Queries: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		LogSolution.logsToQueries(logsPath, queriesFile, onlyComplexQueries);
		log.info("End logs2Queries: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		
		log.info("Start queries2Frequents: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		// Structure work
		LogSolution.patternsToFrequents(queriesFile, freqFile, thresholdQueries);
		LogSolution.sortFrequents(freqFile, sortedFreqFile);
		log.info("End queries2Frequent: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		
		log.info("Start calculating query similarities: ");
//		LogSolution.similarity(sortedFreqFile, simFile, delta);
		LogCluster.executeLimes(sortedFreqFile);
		simFile = LogCluster.DIR_FOR_FILES+File.separator+LogCluster.SIMILARITY_FILE;
		log.info("End calculating query similarities: ");
		
		log.info("Start Clustering...");
		LogCluster.borderFlow(harden, threshold, testOne, heuristic, caching, minNodes, sortedFreqFile, simFile, clusterOutput,  queries);
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
//		if(p.get("delta")!=null){
//			delta=Integer.parseInt(String.valueOf(p.get("delta")).trim());
//		}
//		else{
//			delta=2;
//		}
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

		onlyComplexQueries = Boolean.valueOf(String.valueOf(p.get("only-complex-queries")));
		harden=String.valueOf(p.get("harden")).trim();		
	}

}
