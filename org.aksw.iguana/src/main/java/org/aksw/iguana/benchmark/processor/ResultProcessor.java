package org.aksw.iguana.benchmark.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.utils.ChartGenerator;
import org.aksw.iguana.utils.FileHandler;
import org.aksw.iguana.utils.ResultSet;
import org.aksw.iguana.utils.ResultSetCollection;
import org.aksw.iguana.utils.ZipUtils;
import org.bio_gene.wookie.utils.LogHandler;

/**
 * Processor to arrange, save and order all the results. 
 * 
 * @author Felix Conrads
 *
 */
public class ResultProcessor {

	private static final String RESULT_FOLDER = "results";
	private static final String TEMP_RESULT_FOLDER = "tempResults";
	private static final String SER_RESULTS_FOLDER = "tmp_ser";
	
	private static int suite=0;
	
	private static HashMap<String, String> results;
	
	
	private static Logger log = Logger.getLogger(ResultProcessor.class
			.getSimpleName());

	/**
	 * Initialize the Logger with a file
	 */
	static {
		LogHandler.initLogFileHandler(log,
				ResultProcessor.class.getSimpleName());
	}
	
	/**
	 * Initialize the result folders. 
	 * Delete the folders if they exists 
	 * and create them new
	 */
	public static void init(){
		//Delete the result and temporary result folders
		FileHandler.removeRecursive(getResultFolder());
		FileHandler.removeRecursive(getTempResultFolder());
		FileHandler.removeRecursive(SER_RESULTS_FOLDER);
		//Create the result and temporary result folders
		new File(getResultFolder()).mkdir();
		new File(getTempResultFolder()).mkdir();
		new File(SER_RESULTS_FOLDER).mkdir();
		
		//init the results map new
		results = new HashMap<String, String>();
	}
	
	
	/**
	 * Get the Results of the testcase with a given ID
	 * @param id the testcaseID (The id is the order nr. in the config file)
	 * @return Results for the Testcase with the ID id
	 */
	public static Collection<ResultSet> getResultsForTestcase(String id){
		Collection<ResultSet> ret = null;
		if(!new File(SER_RESULTS_FOLDER+File.separator+id+".results").exists()){
			return null;
		}
        try {
        	FileInputStream fileIn = new FileInputStream(SER_RESULTS_FOLDER+File.separator+id+".results");
            ObjectInputStream in = new ObjectInputStream(fileIn);
			ret = ((ResultSetCollection) in.readObject()).getIntern();
			in.close();
	        fileIn.close();
	        new File(SER_RESULTS_FOLDER+File.separator+id+".results").delete();
		} catch (ClassNotFoundException | IOException e) {
			log.severe("Couldn't load results for testcase id "+id);
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
        
		return ret;
	}
	
	
	private static boolean saveResultsAsObject(String id, ResultSetCollection rsc){
		try {
			FileOutputStream fileOut = new FileOutputStream(SER_RESULTS_FOLDER+File.separator+id+".results");
			ObjectOutputStream output = new ObjectOutputStream(fileOut);
			output.writeObject(rsc);
	        output.close();
	        fileOut.close();
	        rsc.clear();
	        return true;
		} catch (IOException e) {
			log.severe("Couldn't save results!!!!");
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return false;
		}
		
	}
	
	/**
	 * Put the results in the processor for the testcase with the ID id
	 * This will overwrite all results which are previously put for the id
	 * 
	 * @param id The testcase ID
	 * @param testcaseResults The results of the testcase
	 */
	public static void putResultsForTestcase(String id, Collection<ResultSet> testcaseResults){
		ResultSetCollection rsc = new ResultSetCollection();
		
		rsc.setIntern(testcaseResults);
		
		saveResultsAsObject(id, rsc);
		
		rsc = null;
		testcaseResults =null;	
		results.put(id, id+".results");
		
		log.info("Starting Garbage Collector");
		System.gc();
		log.info("Finished Garbage Collector");
		
	}
	
	/**
	 * This will save all results in the Result Folder
	 * and zips the result folder
	 * @param saveResultDiagrams If the ResultSet should be saved as PNG Bar Charts
	 */
	public static void saveResults(boolean saveResultDiagrams, String format){
		//For every Testcases 
		for (String key : results.keySet()) {
			//For every ResultSet for the testcase with the ID key
			log.info("Saving Results for "+key+"...");
			Collection<ResultSet> r = getResultsForTestcase(key);
			for (ResultSet res : r) {
				//Get the name of the testcase + percantage
				String testCase = key.split("&")[0]+key.split("&")[2];
				//Make the testcases to alphanumeric and _
				testCase = testCase.replaceAll("[^A-Za-z0-9.]", "_");
				String fileSep =File.separator;
				
				if(fileSep.equals("\\")){
					fileSep=File.separator+File.separator;
				}
				//Get the folder names
				String[] fileName = res.getFileName().split(fileSep);
				//Get the prefixes
				String[] prefixes = res.getPrefixes();
				//And make them to subfolders
				String suffix="";
				for(String prefix : prefixes){
					suffix+=prefix+File.separator;
				}
				//Make folders in which the ResultSet should be save
				new File("."+File.separator+
						getResultFolder()+
						File.separator+testCase+
						File.separator+suffix).mkdirs();
				//Set filename
				res.setFileName("."+File.separator+
						getResultFolder()+
						File.separator+testCase+
						File.separator+suffix+fileName[fileName.length-1]);

				log.info("... save "+res.getFileName()+" ...");			
				
				try {
					//Save the ResultSet
					res.save();
				} catch (IOException e) {
					log.severe("Couldn't save Result "+res.getFileName()+" for "+key+" due to:");
					LogHandler.writeStackTrace(log, e, Level.SEVERE);
				}
				if(saveResultDiagrams){
					try {
						//Save the ResultSet
						ChartGenerator.saveAsPNG(res, format);
//						res.saveAsPNG(format);
					} catch (Exception e) {
						log.severe("Couldn't save Diagram"+res.getFileName()+" for "+key+" due to:");
						LogHandler.writeStackTrace(log, e, Level.SEVERE);
					}
				}
			}
			log.info("...finished saving results for "+key);
		}
		log.info("Finished saving results");
		//Delete the temp Directory		
		FileHandler.removeRecursive(getTempResultFolder());
		
		try {
			//Try to zip the result folder
			ZipUtils.folderToZip(getResultFolder(), getResultFolder()+".zip");
		} catch (IOException e) {
			log.warning("Couldn't make zip file for "+getResultFolder()+" due to: ");
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
	}


	/**
	 * Sets the current suite nr.
	 * 
	 * @param suite the nr of the current suite
	 */
	public static void setSuite(int suite) {
		ResultProcessor.suite = suite;
	}
	
	/**
	 * Gets the result folder
	 * This contains out of results_suiteNR
	 * 
	 * @return Path of the result folder
	 */
	public static String getResultFolder(){
		return RESULT_FOLDER+"_"+suite;
	}

	/**
	 * Gets the temporary result folder
	 * This contains out of tempResults_suiteNR
	 * 
	 * @return Path of the temporary result folder
	 */
	public static String getTempResultFolder(){
		return TEMP_RESULT_FOLDER+"_"+suite;
	}
	
}
