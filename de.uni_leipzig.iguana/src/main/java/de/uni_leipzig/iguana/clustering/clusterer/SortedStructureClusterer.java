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
 * The Class SortedStructureClusterer.
 * clusters the logs only through their Structure
 * 
 * @author Felix Conrads
 */
public class SortedStructureClusterer implements Clusterer {

	/** The path. */
	private static String PATH = "cluster" + File.separator;
	
	/** The log. */
	private static Logger log = LogSolution.getLogger();
	
	/** The threshold structs. */
	private Integer thresholdStructs=10;
	
	/** The threshold queries. */
	private Integer thresholdQueries=10;

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.clustering.clusterer.Clusterer#cluster(java.lang.String, java.lang.String)
	 */
	@Override
	public void cluster(String logsPath, String queries) throws IOException {
		cluster(logsPath, new File(queries));
	}

	
	/**
	 * Clusters the logs in the logPath to query patterns
	 *
	 * @param logPath the Path with the logFiles
	 * @param queries the file in which the query patterns should be saved
	 * @throws IOException Signals that an IOException has occurred.
	 */
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
		LogSolution.logsToQueries(logsPath, queriesFile, false);
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


	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.clustering.clusterer.Clusterer#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties p) {
		if((thresholdQueries=Integer.parseInt(String.valueOf(p.get("threshold-queries")).trim()))==null){
			thresholdQueries=10;
		}
		if((thresholdStructs=Integer.parseInt(String.valueOf(p.get("threshold-structs")).trim()))==null){
			thresholdStructs=10;
		}
	}

}
