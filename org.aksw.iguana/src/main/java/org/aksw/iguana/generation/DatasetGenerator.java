package org.aksw.iguana.generation;

import java.util.Properties;

import org.bio_gene.wookie.connection.Connection;

public interface DatasetGenerator {

	public void setProperties(Properties p);

	public boolean generateDataset(Connection con, String initalFile, double percent,
			String outputFile);
	
}
