package org.aksw.iguana.generation;

import java.util.Properties;

import org.aksw.iguana.connection.Connection;
import org.aksw.iguana.utils.ShellProcessor;

public abstract class AbstractCommandDatasetGenerator implements DatasetGenerator {

	protected Properties p;
	protected String command;
	
	@Override
	public void setProperties(Properties p) {
		this.p = p;
	}

	protected abstract boolean init(String initialFile, Double percent, String outputFile);
	
	@Override
	public boolean generateDataset(Connection con, String initialFile,
			double percent, String outputFile) {
		if(!init(initialFile, percent, outputFile))
			return false;
		return ShellProcessor.executeCommand(command, p.getProperty("folder"));
	}

}
