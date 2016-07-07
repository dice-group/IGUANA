package org.aksw.iguana.generation.impl;

import org.aksw.iguana.generation.AbstractCommandDatasetGenerator;


public class BSBMDatasetGenerator extends AbstractCommandDatasetGenerator {

	@Override
	protected boolean init(String initialFile, Double percent, String outputFile) {
		this.command="./generate "+"-pc "+percent+" -fn "+outputFile;
		if(p.contains("fc")){
			this.command+=" -fc";
		}
		return true;
	}

}
