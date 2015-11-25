package org.aksw.iguana.analyzer;

import java.io.IOException;
import java.util.Properties;

/**
 * The Interface Analyzer. 
 * 
 * @author Felix Conrads
 */
public interface Analyzer {

	/**
	 * Analyze the logs in the logPath to query patterns
	 *
	 * @param logPath the Path with the logFiles
	 * @param queriesFile the name of the file in which the query patterns should be saved
	 * @throws IOException Signals that an IOException has occurred.
	 */
	public String analyze(String logPath, String queriesFile) throws IOException;
	
	/**
	 * Sets the properties.
	 *
	 * @param p the new properties
	 */
	public void setProperties(Properties p);
}
