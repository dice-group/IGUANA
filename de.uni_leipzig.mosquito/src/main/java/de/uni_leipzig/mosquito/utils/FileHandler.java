package de.uni_leipzig.mosquito.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.utils.LogHandler;

public class FileHandler {

	private static Logger log = Logger.getLogger(FileHandler.class.getName());
	
	static {
		LogHandler.initLogFileHandler(log, FileHandler.class.getSimpleName());
	}
	
	public static long getLineCount(String fileName){
		return getLineCount(new File(fileName));
	}
	
	public static long getLineCount(File file){
		long lines=0;
		BufferedReader br= null;
		try {
			file.createNewFile();
			FileInputStream fis = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			String line ="";
			while((line=br.readLine()) != null){
				if(!line.isEmpty() && !line.equals("\n")){
					lines++;
				}
			}
		} catch (IOException e) {
			lines= -1;
		} finally{
			if(br!=null){
				try {
					br.close();
				} catch (IOException e) {
					LogHandler.writeStackTrace(log, e, Level.WARNING);
				}
			}
		}
		return lines;
	}
	
	public static Collection<String> getSubjectsInFile(String fileName){
		return getSubjectsInFile(new File(fileName));
	}
	
	public static Collection<String> getSubjectsInFile(File file){
		if(!file.exists()){
			return new HashSet<String>();
		}
		Set<String> subjects = new HashSet<String>();
		BufferedReader br= null;
		try {
			file.createNewFile();
			FileInputStream fis = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			String line ="";
			while((line=br.readLine()) != null){
				if(!line.isEmpty() && !line.equals("\n")){
					//TODO
					int begin = line.indexOf("<");
                    int end = line.indexOf(">");
                    String subject = line.substring(begin+1, end-1);
                    
                    if(!subjects.contains(subject)) {
                        log.info("Subject: " + subject);
                        subjects.add(subject);
                    }
				}
			}
		} catch (IOException e) {
			return new HashSet<String>();
		} finally{
			if(br!=null){
				try {
					br.close();
				} catch (IOException e) {
					LogHandler.writeStackTrace(log, e, Level.WARNING);
				}
			}
		}
		return subjects;
	}
	
}
