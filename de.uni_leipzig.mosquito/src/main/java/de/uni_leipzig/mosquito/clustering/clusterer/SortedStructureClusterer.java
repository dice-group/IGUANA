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

public class SortedStructureClusterer implements Clusterer {

	private static String PATH = "cluster" + File.separator;
	private static Logger log = LogSolution.getLogger();
	private Integer thresholdStructs;
	private Integer thresholdQueries;

	@Override
	public void cluster(String logsPath, String queries) throws IOException {
		cluster(logsPath, new File(queries));
	}

	
	public void cluster(String logsPath, File queries) throws IOException {
		String start = DateFormat.getDateTimeInstance().format(new Date());
		Calendar calS = Calendar.getInstance();
		log.info("Starting ClusterProcess " + start);
		new File(PATH).mkdir();
		String queriesFile = PATH + "queryset.log";
		String structFile = PATH + "structs.log";
		String freqStructFile = PATH + "freqStruct.log";
		String sortedFreqStructFile = PATH + "sortedFreqStruct.log";
		String freqFile = PATH + "freq.log";
		String sortedFreqFile = PATH + "sortedFreq.log";

		log.info("Start logs2Queries: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		LogSolution.logsToPatterns(logsPath, queriesFile);
		log.info("End logs2Queries: "
				+ DateFormat.getDateTimeInstance().format(new Date()));

		// Structure work
		log.info("Start queries2FrequentStructs: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		log.info("Start queries2Structure...");
		LogSolution.queriesToStructure(queriesFile, structFile);
		log.info("Start structs2Frequents...");
		LogSolution.patternsToFrequents(structFile, freqStructFile,
				thresholdStructs);
		log.info("Start sorting structs...");
		LogSolution.sortFrequents(freqStructFile, sortedFreqStructFile);
		log.info("End queries2FrequentStructs: "
				+ DateFormat.getDateTimeInstance().format(new Date()));

		log.info("Start queries2Frequents: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		// Structure work
		LogSolution
				.patternsToFrequents(queriesFile, freqFile, thresholdQueries);
		LogSolution.sortFrequents(freqFile, sortedFreqFile);
		log.info("End queries2Frequent: "
				+ DateFormat.getDateTimeInstance().format(new Date()));

		// Clustering
		log.info("Start Clustering: "
				+ DateFormat.getDateTimeInstance().format(new Date()));
		LogCluster.sortedStructure(new File(sortedFreqFile),
				new File(sortedFreqStructFile), queries);
		String end = DateFormat.getDateTimeInstance().format(new Date());
		Calendar calE = Calendar.getInstance();
		log.info("Ended ClusterProcess " + end);
		log.info("Needed Time: "
				+ EmailHandler.getWellFormatDateDiff(calS, calE));
	}

}
