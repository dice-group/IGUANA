package org.aksw.iguana.testcases;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.aksw.iguana.utils.ResultSet;
import org.bio_gene.wookie.connection.Connection;
import org.w3c.dom.Node;

/**
 * Testcase Interface to test several Cases easily in the Benchmark.
 *
 * @author Felix Conrads
 */
public interface Testcase {
	
	/**
	 * Starts the testcase
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void start() throws IOException;
	
	/**
	 * Gets the results.
	 *
	 * @return the results
	 */
	public Collection<ResultSet> getResults();
	
	/**
	 * Adds the current results.
	 *
	 * @param currentResults the current results
	 */
	public void addCurrentResults(Collection<ResultSet> currentResults);
	
	/**
	 * Sets the properties.
	 *
	 * @param p the new properties
	 */
	public void setProperties(Properties p);
	
	/**
	 * Sets the connection.
	 *
	 * @param con the new connection
	 */
	public void setConnection(Connection con);
	
	
	public void setConnectionNode(Node con, String id);
	/**
	 * Sets the current db name.
	 *
	 * @param name the new current db name
	 */
	public void setCurrentDBName(String name);

	/**
	 * Sets the current percentage.
	 *
	 * @param percent the new current percentage
	 */
	public void setCurrentPercent(String percent);
	
	public Boolean isOneTest();
}
