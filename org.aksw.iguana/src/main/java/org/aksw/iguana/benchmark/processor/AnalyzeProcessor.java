package org.aksw.iguana.benchmark.processor;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.analyzer.Analyzer;
import org.aksw.iguana.utils.logging.LogHandler;

/**
 * Processor for log clustering
 * 
 * Does all the work to start the Clusterer
 * 
 * @author Felix Conrads
 *
 */
public class AnalyzeProcessor {

	private static Logger log = Logger.getLogger(AnalyzeProcessor.class
			.getSimpleName());

	/**
	 * initialize the Logger with a file
	 */
	static {
		LogHandler.initLogFileHandler(log,
				AnalyzeProcessor.class.getSimpleName());
	}
	
	/**
	 * 
	 * @param name Class name of the clusterer
	 * @param logPath Path or filename of the log file(s)
	 * @param queriesFile The output queries file
	 * @param logCluster All the Properties the clusterer should use
	 * @return file name of the resulting Query file (Can be null of problems occured)
	 */
	public static String clustering(String name, String logPath, String queriesFile, Properties logCluster){
		try {
		
			//Initalize the logcluster class
			Analyzer cl = (Analyzer) Class.forName(name).newInstance();
			//Set Properties
			cl.setProperties(logCluster);
			//Cluster the log files
			return cl.analyze(logPath, queriesFile);

		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException | IOException e) {
			log.severe("LogClusterer had some problems due to: ");
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		return null;
	}
}
