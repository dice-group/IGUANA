package de.uni_leipzig.mosquito.testcases;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.uni_leipzig.mosquito.utils.ResultSet;

public class StressTestcase implements Testcase {
	
	private int users;
	private int minRandom;
	private int maxRandom;
	private int minRandomThreadDelay;
	private int maxRandomThreadDelay;
	private List<String> selectQueries;
	private List<String> updateQueries;
	private List<File> files;
	
	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<ResultSet> getResults() {
		return null;
		// TODO Auto-generated method stub

	}

	@Override
	public void setProperties(Properties p) {
		users = Integer.parseInt(p.getProperty("users", "20"));
	}

	@Override
	public void setCurrentResults(Collection<ResultSet> currentResults) {
		// TODO Auto-generated method stub
		
	}

}
