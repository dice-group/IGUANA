package org.aksw.iguana.generation.impl;

import java.io.File;
import java.io.IOException;

import org.aksw.iguana.connection.Connection;
import org.aksw.iguana.generation.AbstractCommandDatasetGenerator;
import org.aksw.iguana.utils.ShellProcessor;
import com.google.common.io.Files;

public class WatDifDatasetGenerator extends AbstractCommandDatasetGenerator {

	@Override
	protected boolean init(String initialFile, Double percent, String outputFile) {
		this.command="./watdiv -d "+initialFile+" "+percent.intValue();	//initialFile=watDifmodel; 1=percent: 100k triples 	
		return true;
	}

	@Override
	public boolean generateDataset(Connection con, String initialFile,
			double percent, String outputFile) {
		init(initialFile, percent, outputFile);
		Boolean ret = ShellProcessor.executeCommand(initialFile, p.getProperty("folder"));
		try {
			Files.move(new File(p.getProperty("folder")+File.separator+"saved.txt"), new File(outputFile));
		} catch (IOException e) {
			return false;
		}
		return ret;
	}
}
