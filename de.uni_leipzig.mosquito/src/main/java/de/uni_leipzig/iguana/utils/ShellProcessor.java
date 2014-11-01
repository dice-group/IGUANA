package de.uni_leipzig.iguana.utils;

import org.bio_gene.wookie.utils.CurlProcess;

public class ShellProcessor extends CurlProcess{
	
	private static long waitForIt=5*60000;
	
	public static Boolean executeCommand(String command){
		ShellProcessor sp = new ShellProcessor();
		Boolean ret = sp.process(command);
		try {
			Thread.sleep(waitForIt);
		} catch (InterruptedException e) {
			return ret;
		}
		return ret;
	}
	
}
