package org.aksw.iguana.generation.impl;

import org.aksw.iguana.generation.AbstractCommandDatasetGenerator;

public class SP2BenchDatasetGenerator extends AbstractCommandDatasetGenerator {

	@Override
	protected boolean init(String initialFile, Double percent, String outputFile) {
		this.command="./sp2b";
		if(System.getProperty("os.name").startsWith("Windows")){
			this.command+=".exe";
		}
		else{
			this.command+="_gen";
		}
		
		 if("s".equals(p.getProperty("break_condition"))){
			 this.command+=" -s "+percent+" ";
		 }
		 else{
			 this.command+=" -t "+percent+" ";
		 }
		
		this.command+=outputFile; 
		return true;
	}

}
