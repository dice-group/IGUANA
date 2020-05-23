package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import org.aksw.iguana.tp.config.CONSTANTS;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

public class CLIInputFileWorker extends MultipleCLIInputWorker {


	private String dir;

    @Override
	public void init(Properties p) {
		super.init(p);
		this.dir = p.getProperty(CONSTANTS.DIRECTORY);
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
