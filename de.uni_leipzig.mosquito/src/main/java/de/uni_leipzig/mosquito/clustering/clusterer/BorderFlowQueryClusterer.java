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

public class BorderFlowQueryClusterer implements Clusterer {

	public static String PATH = "cluster" + File.separator;
	private static Logger log = LogSolution.getLogger();
	private Integer thresholdQueries=10;
	private Integer delta=2;
	private String harden=null;
	private String quality=null;
	private Double threshold=0.8;
	private Boolean testOne=true, heuristic=true, caching=true;
	private Integer minNodes;
	
	public static void main(String[] argc){
		BorderFlowQueryClusterer bfqc = new BorderFlowQueryClusterer();
		bfqc.setProperties(new Properties());
		try {
			bfqc.cluster("../../LogFiles", "Queries.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

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
		String clQueryOutput = PATH +"choosenClusterQuery.log";
		String simFile = PATH + "similarity.log";
		
		log.info("Start logs2Queries: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		LogSolution.logsToPatterns(logsPath, queriesFile);
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
		LogSolution.similarity(sortedFreqFile, simFile, delta);
		log.info("End calculating query similarities: ");
		
		log.info("Start Clustering...");
		LogCluster.borderFlow(harden, quality, threshold, testOne, heuristic, caching, minNodes, sortedFreqFile, simFile, clusterOutput, clQueryOutput, queries);
		String end = DateFormat.getDateTimeInstance().format(new Date());
		Calendar calE = Calendar.getInstance();
		log.info("Ended ClusterProcess " + end);
		log.info("Needed Time: "
				+ EmailHandler.getWellFormatDateDiff(calS, calE));
		
	}


	@Override
	public void setProperties(Properties p) {
		if(p.get("threshold-queries")!=null){
			thresholdQueries=Integer.parseInt(String.valueOf(p.get("threshold-queries")).trim());
		}
		else{
			thresholdQueries=10;
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
