package org.aksw.iguana.benchmark.processor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.aksw.iguana.connection.Connection;
import org.aksw.iguana.generation.DatasetGenerator;
import org.aksw.iguana.utils.Config;
import org.aksw.iguana.utils.logging.LogHandler;


/**
 * Processor to generate smaller and bigger datasets
 * 
 * @author Felix Conrads
 *
 */
public class DatasetGeneratorProcessor {
	
	private static Logger log = Logger.getLogger(DatasetGeneratorProcessor.class
			.getSimpleName());
	
	private static Map<String, String[]> map = new HashMap<String, String[]>();

	/**
	 * initialize the Logger with a file
	 */
	static {
		LogHandler
				.initLogFileHandler(log, DatasetGeneratorProcessor.class.getSimpleName());
	}
	
	/**
	 * Generates smaller and bigger files with a reference conneciton and an initial 
	 * 100% file
	 * 
	 * @param con The reference Connection which should be used
 	 * @param hundredFile The initial 100% filename
	 * @return List of all the filenames 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 */
	public static String[] getDatasetFiles(Connection con, String datasetGen, String hundredFile, Properties p) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		@SuppressWarnings("unchecked")
		Class<DatasetGenerator> clazz = (Class<DatasetGenerator>) Class
				.forName(datasetGen);
		DatasetGenerator dataGen = clazz.newInstance();

		dataGen.setProperties(p);
		//If the dataset is cached (an earlier suite needed the same)
		if(map.containsKey(hundredFile+""+Config.datasetPercantage.hashCode())){
			log.info("Datasets for file"+hundredFile+" is cached, use cached files");
			//return the cached 
			return map.get(hundredFile+""+Config.datasetPercantage.hashCode());
		}
		//Init an array with the size of all the datasets needed
		String[] ret = new String[Config.datasetPercantage.size()];
		//mkdir dataset/ in the current folder
		log.fine("Create directory datasets/ in the current folder");
		new File("datasets" + File.separator).mkdir();
		
		for (int i = 0; i < Config.datasetPercantage.size(); i++) {
			if (Config.datasetPercantage.get(i) == 1.0) {
				log.info("Current Precantage is 100%. Add "+hundredFile+" to the returning files");
				ret[i] = hundredFile;
				continue;
			}
			//Get Percantage
			Double per = Config.datasetPercantage.get(i) * 100.0;
			//outputFile will be 
			String outputFile = "datasets" + File.separator + "ds_" + per
					+ ".nt";
			log.info("Current Percantage is "+per+", generate file "+outputFile);
			if(dataGen.generateDataset(con, hundredFile, Config.datasetPercantage.get(i), outputFile)){
				ret[i] = outputFile;
				log.info("Written " + Config.datasetPercantage.get(i) * 100 + "% Dataset to File");
			}
			else{
				log.severe("Couldn't wrote "+ Config.datasetPercantage.get(i) * 100 + "% Dataset to File");
			}
		}
		//Cache files
		log.info("Cache files for later suites");
		map.put(hundredFile+""+Config.datasetPercantage.hashCode(), ret);
		
		return ret;
	}

}
