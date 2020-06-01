package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;


import org.aksw.iguana.tp.config.CONSTANTS;

import java.util.Properties;

public class CLIInputPrefixWorker extends MultipleCLIInputWorker {

	private String prefix;
	private String suffix;


    @Override
	public void init(Properties p) {
		super.init(p);
		this.prefix = p.getProperty(CONSTANTS.QUERY_PREFIX);
		this.suffix = p.getProperty(CONSTANTS.QUERY_SUFFIX);
	}

	@Override
	protected String writableQuery(String query) {
		return prefix+" "+query+" "+suffix;
	}

}
