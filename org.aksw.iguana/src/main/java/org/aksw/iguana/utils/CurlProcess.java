package org.aksw.iguana.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.utils.logging.LogHandler;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

/**
 * 
 * Hilfsklasse um Curl Prozesse zu erstellen und starten. 
 * 
 * @author Felix Conrads
 *
 */
public class CurlProcess {
	
	private Logger log = Logger.getLogger(CurlProcess.class.getSimpleName());
	
	public CurlProcess(){
		LogHandler.initLogFileHandler(log, CurlProcess.class.getSimpleName());
	}
	
	protected void setLogger(Logger log){
		this.log = log;
	}
	
	protected File setData(String data, String suffix){
		//uuid timestamp script
		UUID gen = UUID.randomUUID();
		File script = new File(String.valueOf(gen.toString())+suffix);
		//duplicate avoidance
		while(script.exists()){
			//try to wait  a short random time 
			try {
				wait((int)(Math.random()*100));
			} catch (InterruptedException e) {
				LogHandler.writeStackTrace(log, e, Level.WARNING);
			}
			gen = UUID.randomUUID();
			script = new File(String.valueOf(gen.timestamp()));
		}
		//Writes data to script
		PrintWriter pw = null;
		try {
			script.createNewFile();
			pw = new PrintWriter(script);
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		pw.write(data);
		pw.close();
		return script;
	}
	
	protected Boolean process(String command){
//		File script;
//		String suffix = ".sh";
//		if(SystemUtils.IS_OS_WINDOWS){
//			suffix = ".bat";
//		}
//		if((script= setData(command, suffix))==null){
//			return false;
//		}
//		script.setExecutable(true);
//		//starts and wait for the process
//		ProcessBuilder pb = new ProcessBuilder("."+File.separator+script.getName());
//		pb.directory(new File(script.getAbsolutePath().replace(
//				script.getName(), File.separator)));
		try {
			   log.info(command);
			   CommandLine cmdLine = CommandLine.parse(command);
			   DefaultExecutor exec = new DefaultExecutor();
			   int ret = exec.execute(cmdLine);
//			   Process p = pb.start();
//			   StreamGrabber error = new StreamGrabber(p.getErrorStream(), Level.SEVERE, log);
//			   StreamGrabber info = new StreamGrabber(p.getInputStream(), Level.INFO, log);
//			   error.start();
//			   info.start();
//			   int ret = p.waitFor();
			   log.info("Return value of Process: "+ret);
//			   p = null;
//			   Runtime.getRuntime().gc();
			  
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return false;
		}
		//deletes the script
//		script.delete();
		return true;
	}
}
