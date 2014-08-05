package de.uni_leipzig.mosquito.clustering.clusterer;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import de.uni_leipzig.mosquito.clustering.LogCluster;
import de.uni_leipzig.mosquito.clustering.LogSolution;
import de.uni_leipzig.mosquito.utils.EmailHandler;

public class BorderFlowStructureClusterer implements Clusterer {

	public static String PATH = "cluster" + File.separator;
	private static Logger log = LogSolution.getLogger();
	private Integer thresholdQueries=10;
	private int delta=2;


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
		String clusterOutput = PATH +"cluster.log";
		String clQueryOutput = PATH +"choosenClusterQuery.log";
		String simFile = PATH + "similarity.log";
		String queriesStruct = PATH + "choosenStructs.log";
		
		log.info("Start logs2Queries: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		LogSolution.logsToPatterns(logsPath, queriesFile);
		log.info("End logs2Queries: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		
		log.info("Start queries2Structs");
		LogSolution.queriesToStructure(queriesFile, structs);
		log.info("End queries2Structs"
			+ DateFormat.getDateTimeInstance().format(new Date()));
		log.info("Start structs2Frequents: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		// Structure work
		LogSolution.patternsToFrequents(structs, freqFile, thresholdQueries);
		LogSolution.sortFrequents(freqFile, sortedFreqFile);
		log.info("End structs2Frequent: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		
		log.info("Start calculating query similarities: ");
		LogSolution.similarity(sortedFreqFile, simFile, delta);
		log.info("End calculating query similarities: "+ DateFormat.getDateTimeInstance().format(new Date()));
		
		
		//TODO Match Structs to most freq. queries in queriesStruct
		
		
		log.info("Start Clustering...");
		LogCluster.borderFlow(queriesStruct, simFile, clusterOutput, clQueryOutput, queries);
		String end = DateFormat.getDateTimeInstance().format(new Date());
		Calendar calE = Calendar.getInstance();
		log.info("Ended ClusterProcess " + end);
		log.info("Needed Time: "
				+ EmailHandler.getWellFormatDateDiff(calS, calE));
		
		
		
	}

}
