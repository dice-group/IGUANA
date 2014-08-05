package de.uni_leipzig.mosquito.clustering.clusterer;

import java.io.IOException;

public interface Clusterer {

	public void cluster(String logPath, String queriesFile) throws IOException;
}
