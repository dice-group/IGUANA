package org.aksw.iguana.query.impl;

import java.io.IOException;

import org.aksw.iguana.query.AbstractQueryHandler;
import org.aksw.iguana.utils.ShellProcessor;

public class BSBMQueryHandler extends AbstractQueryHandler{

	
	@Override
	public void init() throws IOException {
		// TODO Write command
		String command = "./watdiv -q ";
		
		//TODO Execute command 
		ShellProcessor.executeCommand(command, this.pattern);
		
		//TODO get queries from whatever to path
		
	}

}
