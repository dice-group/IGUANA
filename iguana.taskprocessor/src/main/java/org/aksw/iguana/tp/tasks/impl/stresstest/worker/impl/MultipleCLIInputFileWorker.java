package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class MultipleCLIInputFileWorker extends MultipleCLIInputWorker {


	private String dir;
	
	@Override
	public void init(String args[]) {
		super.init(args);
		int i=13;
		if(args.length>13) {
			processList = new Process[Integer.parseInt(args[13])];
			i++;
		}
		this.dir = args[i];

	}
	
	@Override
	protected String writableQuery(String query) {
		File f;
		
		try {
			f = new File(dir+File.separator+"tmpquery.sparql");
			f.deleteOnExit();
			try(PrintWriter pw = new PrintWriter(f)){
				pw.print(query);
			}
			return f.getName();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return query;
	}
	
}
