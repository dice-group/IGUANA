package org.aksw.iguana.utils.logging;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Easy Log Handling
 *
 * @author Felix Conrads
 */
public class LogHandler {

	private static HashMap<String, FileHandler> fileMap = new HashMap<String, FileHandler>();
	
	/**
	 * tries to add a FileHandler to the Logger with the given Name
	 * otherwise it will log the exception
	 * 
	 * @param log
	 * @param logName
	 * @return true wenn erfolgreich, ansonsten false
	 */
	public static Boolean initLogFileHandler(Logger log, String logName){
		try {
			//Ordner log gegebenfalls erstellen
			File file = new File("logs");
			file.mkdirs();
			FileHandler fh = fileMap.get(logName);
			//Datei ./log/Connection.log mit Größe der max. Value eines Integer, max. 3 Dateien und append
			if(fh == null){
				fh = new FileHandler(file.getAbsolutePath()+File.separator+logName+".log", 400000, 3, true );
				fileMap.put(logName, fh);
			}
			else{
				return true;
			}
			ConsoleHandler ch = new ConsoleHandler();
			LogFormatter formatter = new LogFormatter();
			ch.setFormatter(formatter);
			fh.setFormatter(formatter);
			//Logger schreibt somit in die Datei
			log.addHandler(fh);
			log.addHandler(ch);
			log.setUseParentHandlers(false);
			
			return true;
		} catch (SecurityException | IOException e) {
			//Logger schreibt StackTrace der Exception in die Konsole/Log-File
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			log.warning(sw.toString());
			return false;
		}
	}

	
	/**
	 * Writes the StackTrace into the Logger
	 * 
	 * @param log
	 * @param e
	 * @param lvl
	 */
	public static void writeStackTrace(Logger log, Exception e, Level lvl){
		try{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			log.log(lvl, sw.toString());
		}catch(Exception e1){
			if(e!=null)
				System.out.println(e);
			System.out.println(e1);
		}
	}
	
}
