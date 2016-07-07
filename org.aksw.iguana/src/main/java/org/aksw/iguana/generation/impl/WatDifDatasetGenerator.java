package org.aksw.iguana.generation.impl;

import org.aksw.iguana.generation.AbstractCommandDatasetGenerator;

public class WatDifDatasetGenerator extends AbstractCommandDatasetGenerator {

	@Override
	protected boolean init(String initialFile, Double percent, String outputFile) {
		this.command="./watdiv -d "+initialFile+" "+percent.intValue();	//initialFile=watDifmodel; 1=percent: 100k triples 	
		return true;
	}

}
