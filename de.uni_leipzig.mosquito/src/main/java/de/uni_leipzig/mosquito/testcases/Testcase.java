package de.uni_leipzig.mosquito.testcases;

import java.util.Collection;
import java.util.Properties;

import de.uni_leipzig.mosquito.utils.ResultSet;

/**
 * Testcase Interface to test several Cases easily in the Benchmark
 * 
 * 
 * @author Felix Conrads
 *
 */
public interface Testcase {
	
	public void start();
	
	public Collection<ResultSet> getResults();
	
	public void setCurrentResults(Collection<ResultSet> currentResults);
	
	public void setProperties(Properties p);

}
