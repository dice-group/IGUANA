package org.aksw.iguana.benchmark.processor;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.clustering.clusterer.Clusterer;
import org.bio_gene.wookie.utils.LogHandler;

public class ClusterProcessor {

	private static Logger log = Logger.getLogger(ClusterProcessor.class
			.getSimpleName());

	static {
		LogHandler.initLogFileHandler(log,
				ClusterProcessor.class.getSimpleName());
	}
	
	public static String clustering(String name, String logPath, String queriesFile, Properties logCluster){
		try {
			Clusterer cl = (Clusterer) Class.forName(name).newInstance();
			cl.setProperties(logCluster);
			return cl.cluster(logPath, queriesFile);
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException | IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		return null;
	}
}
