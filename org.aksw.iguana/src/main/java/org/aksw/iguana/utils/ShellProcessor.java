package org.aksw.iguana.utils;

import org.aksw.iguana.utils.CurlProcess;

public class ShellProcessor extends CurlProcess{
	
	private static long waitForIt=5*10000;
	
	public static long getWaitForIt() {
		return waitForIt;
	}

	public static void setWaitForIt(long waitForIt) {
		ShellProcessor.waitForIt = waitForIt;
	}

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
	
	public static Boolean executeCommand(String command, String dir){
		ShellProcessor sp = new ShellProcessor();
	
		Boolean ret = sp.process(command, dir);
		try {
			Thread.sleep(waitForIt);
		} catch (InterruptedException e) {
			return ret;
		}
		return ret;
	}
	
	public static Boolean executeCommand(String command, Long wait){
		ShellProcessor sp = new ShellProcessor();
	
		Boolean ret = sp.process(command);
		try {
			Thread.sleep(wait);
		} catch (InterruptedException e) {
			return ret;
		}
		return ret;
	}
	
}
