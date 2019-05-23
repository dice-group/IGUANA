package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;


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
	protected String writableQuery(String query) {
		return prefix+" "+query+" "+suffix;
	}

}
