package de.uni_leipzig.mosquito.clustering.clusterer;

import java.io.IOException;
import java.util.Properties;

public interface Clusterer {

	public void cluster(String logPath, String queriesFile) throws IOException;
	
	public void setProperties(Properties p);
}
