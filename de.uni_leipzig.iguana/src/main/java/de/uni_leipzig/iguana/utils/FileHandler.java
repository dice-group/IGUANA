package de.uni_leipzig.iguana.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.utils.LogHandler;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


/**
 * Provides some functions to easily work with files 
 * 
 * @author Felix Conrads
 */
public class FileHandler {

	/** The log. */
	private static Logger log = Logger.getLogger(FileHandler.class.getName());
	
	static {
		LogHandler.initLogFileHandler(log, FileHandler.class.getSimpleName());
	}
	
	public static void writeLinesToFile(String fileName, Collection<String> lines) throws IOException{
		writeLinesToFile(new File(fileName), lines);
	}
	
	public static void writeLinesToFile(File file, Collection<String> lines) throws IOException{
		file.createNewFile();
		PrintWriter pw = new PrintWriter(file);
		for(String line : lines){
			pw.println(line);
		}
		try{
			pw.close();
		}catch(Exception e){}
	}
	
	/**
	 * Write files in a given directory to one file.
	 *
	 * @param dir the dir name with the files 
	 * @param f the output file name
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void writeFilesToFile(String dir, String f) throws IOException{
		writeFilesToFile(new File(dir), new File(f));
	}
	
	/**
	 * Write files in a given directory to one file.
	 *
	 * @param dir the dir with the files
	 * @param f the output file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void writeFilesToFile(File dir, File f) throws IOException{
		f.createNewFile();
		String line;
		PrintWriter pw = new PrintWriter(f);
		for(File file : dir.listFiles()){
			String ntFile = file.getAbsolutePath();
			FileInputStream fis = new FileInputStream(file);
			if(!file.getName().endsWith(".nt")&&!file.getName().endsWith(".n3")){
				Model m = ModelFactory.createDefaultModel();
				m.read(fis, null);
				ntFile = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf("."))+".nt";
				new File(ntFile).createNewFile();
				m.write(new FileOutputStream(ntFile), "N-TRIPLE");
			}
			FileInputStream fis2 = new FileInputStream(ntFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis2, Charset.forName("UTF-8")));

			while((line=br.readLine())!=null){
				pw.println(line);
			}
			br.close();
			fis.close();
			pw.flush();
		}
		pw.close();
	}
	
	/**
	 * Gets the line count of a given file
	 *
	 * @param fileName the file name
	 * @return the line count
	 */
	public static long getLineCount(String fileName){
		return getLineCount(new File(fileName));
	}
	
	/**
	 * Gets the line count of a given file
	 *
	 * @param file the file
	 * @return the line count
	 */
	public static long getLineCount(File file){
		long lines=0;
		FileInputStream fis = null;
		BufferedReader br= null;
		try {
			if(!file.exists())
				file.createNewFile();
			fis = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			String line ="";
			while((line=br.readLine()) != null){
				if(!line.isEmpty() && !line.equals("\n")){
					lines++;
				}
			}
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.WARNING);
			lines= -1;
		} finally{
			if(br!=null){
				try {
					if(br!=null)		
						br.close();
				} catch (IOException e) {
					LogHandler.writeStackTrace(log, e, Level.WARNING);
				}
				try {
					if(fis!=null)
						fis.close();
				} catch (IOException e) {
					LogHandler.writeStackTrace(log, e, Level.WARNING);
				}
			}
		}
		return lines;
	}
	
	
	public static Collection<String> getLines(String fileName){
		return getLines(new File(fileName));
	}
	
	/**
	 * Gets the line count of a given file
	 *
	 * @param file the file
	 * @return the line count
	 */
	public static Collection<String> getLines(File file){
		BufferedReader br= null;
		FileInputStream fis=null;
		LinkedList<String> ret = new LinkedList<String>();
		try {
			file.createNewFile();
			fis = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			String line ="";
			while((line=br.readLine()) != null){
				if(!line.isEmpty() && !line.equals("\n")){
					ret.add(line);
				}
			}
		} catch (IOException e) {
		} finally{
			if(br!=null){
				try {
					br.close();
				} catch (IOException e) {
					LogHandler.writeStackTrace(log, e, Level.WARNING);
				}
			}
			if(fis!=null){
				try {
					fis.close();
				} catch (IOException e) {
					LogHandler.writeStackTrace(log, e, Level.WARNING);
				}
			}
		}
		return ret;
	}

	/**
	 * Gets the subjects in a given file.
	 *
	 * @param fileName the file name
	 * @return the subjects in file
	 */
	public static Collection<String> getSubjectsInFile(String fileName){
		return getSubjectsInFile(new File(fileName));
	}
	
	/**
	 * Gets the subjects in a given file.
	 *
	 * @param file the file
	 * @return the subjects in file
	 */
	public static Collection<String> getSubjectsInFile(File file){
		if(!file.exists()){
			return new HashSet<String>();
		}
		Set<String> subjects = new HashSet<String>();
		FileInputStream fis = null;
		BufferedReader br= null;
		try {
			file.createNewFile();
			fis = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			String line ="";
			while((line=br.readLine()) != null){
				if(!line.isEmpty() && !line.equals("\n")){
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
			if(fis!=null){
				try {
					fis.close();
				} catch (IOException e) {
					LogHandler.writeStackTrace(log, e, Level.WARNING);
				}
			}
		}
		return subjects;
	}

	/**
	 * Gets the line at index of a given file
	 *
	 * @param fileName the file name
	 * @param index the index
	 * @return the line at index
	 */
	public static String getLineAt(String fileName, int index){
		return getLineAt(new File(fileName), index);
	}
	
	/**
	 * Gets the line at index of a given file
	 *
	 * @param fileName the file
	 * @param index the index
	 * @return the line at index
	 */
	public static String getLineAt(File file, int index){
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		try{
			fis = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			int i =0;
			while((line = br.readLine())!= null && i<index){
				i++;
			}
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
		finally{
			if(br != null){
				try {
					br.close();
				} catch (IOException e) {
					LogHandler.writeStackTrace(log, e, Level.SEVERE);
				}
			}
			if(fis != null){
				try {
					fis.close();
				} catch (IOException e) {
					LogHandler.writeStackTrace(log, e, Level.SEVERE);
				}
			}
		}
		return line;
	}
	
	/**
	 * Gets the queries in a given file.
	 *
	 * @param fileName the file name
	 * @return the queries in file
	 */
	public static Collection<String> getQueriesInFile(String fileName){
		return getQueriesInFile(new File(fileName));
	}
	
	/**
	 * Gets the queries in a given file.
	 *
	 * @param file the file
	 * @return the queries in file
	 */
	public static Collection<String> getQueriesInFile(File file){
		Collection<String> queries = new LinkedList<String>();
		FileInputStream fis  = null;
		BufferedReader br= null;
		try {
			file.createNewFile();
			fis = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			String line ="";
			while((line=br.readLine()) != null){
				if(!line.isEmpty() && !line.equals("\n")){
					queries.add(line);
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
			if(fis!=null){
				try {
					fis.close();
				} catch (IOException e) {
					LogHandler.writeStackTrace(log, e, Level.WARNING);
				}
			}
		}
		
		return queries;
	}
	
	/**
	 * Gets the file count in a given directory
	 *
	 * @param path the path of the dir
	 * @return the file count in dir
	 */
	public static Long getFileCountInDir(String path){
		File p = new File(path);
		if(!p.isDirectory()){
			return -1L;
		}
		int ret=0;
		for(File f : p.listFiles()){
			String test = FileHandler.getLineAt(f, 0);
			if(test!=null&&!test.isEmpty()){
				ret++;
			}
		}
		return ret*1L;
	}	

	/**
	 * Gets all names of files in dir.
	 *
	 * @param path the path of the dir
	 * @return all filenames in dir
	 */
	public static String[] getAllNamesInDir(String path){
		File p = new File(path);
		File[] retF = p.listFiles();
		String[] ret = new String[retF.length];
		String fileSep =File.separator;
		if(fileSep.equals("\\")){
			fileSep=File.separator+File.separator;
		}
		for(int i=0; i<ret.length;i++){
			ret[i] = (retF[i].getName()).replaceAll("(\\.\\w+)$", "").replaceAll("^(.*"+fileSep+")", "");
		}
		return ret;
	}
	
	/**
	 * Gets the filename in dir at pos.
	 *
	 * @param path the path
	 * @param pos the position
	 * @return the name in dir at pos
	 */
	public static String getNameInDirAtPos(String path, int pos){
		String fileSep =File.separator;
		if(fileSep.equals("\\")){
			fileSep=File.separator+File.separator;
		}
		File p = new File(path);
		return p.listFiles()[pos].getName().replaceAll("(\\.\\w+)$", "").replaceAll("^(.*"+fileSep+")", "");
	}
	
	/**
	 * Gets the regex: (extension[1]|extension[2]|...| extension[n])
	 *
	 * @param extensions the extensions
	 * @return the regex
	 */
	private static String getRegex(String[] extensions){
		String regex = "(";
		for(String ext : extensions){
			regex+="\\."+ext+"|";
		}
		regex = regex.substring(0, regex.length()-1)+")";
		return regex;
	}
	
	/**
	 * Gets the files in dir wich ends with one of the given extensions.
	 *
	 * @param path the path of the dir
	 * @param extensions the extensions
	 * @return the files in dir
	 */
	public static Collection<File> getFilesInDir(String path, String[] extensions){
		File root = new File(path);
		final String regex = ".*"+getRegex(extensions)+"$";
		Collection<File> ret = new LinkedList<File>();
		for(String f : root.list()){
			File file = new File(root.getPath()+File.separator+f);
			if(file.isDirectory()){
				ret.addAll(getFilesInDir(root.getPath()+File.separator+f, extensions));
			}
			else{
				if(f.matches(regex))	
					ret.add(file);
			}
		}
		return ret;
	}
	
	
}
