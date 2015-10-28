package de.uni_leipzig.iguana.clustering.clusterer;

import java.io.IOException;
import java.util.Properties;

/**
 * The Interface Clusterer. 
 * 
 * @author Felix Conrads
 */
public interface Clusterer {

	/**
	 * Clusters the logs in the logPath to query patterns
	 *
	 * @param logPath the Path with the logFiles
	 * @param queriesFile the name of the file in which the query patterns should be saved
	 * @throws IOException Signals that an IOException has occurred.
	 */
	public String cluster(String logPath, String queriesFile) throws IOException;
	
	/**
	 * Sets the properties.
	 *
	 * @param p the new properties
	 */
	public void setProperties(Properties p);
}
