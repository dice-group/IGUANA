package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;


import org.aksw.iguana.tp.config.CONSTANTS;

import java.util.Properties;

public class CLIInputPrefixWorker extends MultipleCLIInputWorker {

	private String prefix;
	private String suffix;


	@Override
	public void init(String args[]) {
		super.init(args);
		int i=13;
		if(args.length>15) {
			processList = new Process[Integer.parseInt(args[13])];
			i++;
		}
		this.prefix = args[i];
		this.suffix = args[i+1];

	}

	@Override
	public void init(Properties p) {
		super.init(p);
		if(p.containsKey(CONSTANTS.NO_OF_PROCESSES))
		{
			processList = new Process[Integer.parseInt(p.getProperty(CONSTANTS.NO_OF_PROCESSES))];
		}
		this.prefix = p.getProperty(CONSTANTS.QUERY_PREFIX);
		this.suffix = p.getProperty(CONSTANTS.QUERY_SUFFIX);
	}

	@Override
	protected String writableQuery(String query) {
		return prefix+" "+query+" "+suffix;
	}

}
