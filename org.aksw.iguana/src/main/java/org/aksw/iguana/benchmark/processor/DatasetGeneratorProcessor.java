package org.aksw.iguana.benchmark.processor;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.data.TripleStoreHandler;
import org.aksw.iguana.generation.DataGenerator;
import org.aksw.iguana.generation.DataProducer;
import org.aksw.iguana.generation.ExtendedDatasetGenerator;
import org.aksw.iguana.utils.Config;
import org.aksw.iguana.utils.ExternalSort;
import org.aksw.iguana.utils.FileHandler;
import org.aksw.iguana.utils.comparator.TripleComparator;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

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
	 */
	public static String[] getDatasetFiles(Connection con, String hundredFile) {
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

		String fileName = hundredFile;
		//Test if hundredFile doesn't exists is not set or is a directory
		if (hundredFile == null || !(new File(hundredFile).exists())
				|| new File(hundredFile).isDirectory()) {

			//Init file name of creating one file is: datasets/ds_100.nt
			fileName = "datasets" + File.separator + "ds_100.nt";
			if (new File(hundredFile).isDirectory()) {
				log.info("Initial 100% file is a directory");
				log.info("Try to write all files in the directory together");
				try {
					//Tries to write alle files in the directory together
					FileHandler.writeFilesToFile(hundredFile, fileName);
				} catch (IOException e) {
					log.severe("Couldn't write all files together due to: ");
					LogHandler.writeStackTrace(log, e, Level.SEVERE);
					return null;
				}
			} else {
				log.info("Writing 100% Dataset from reference connection with endpoint "+con.getEndpoint()+" to File");
				/* Gets all triples of the reference connection in the given graph
				 * or the default graph ig graphURI is null or empty. to the file datasets/ds_100.nt
				 */
				TripleStoreHandler.writeDatasetToFile(con,
						Config.graphURI, fileName);
			}
		}
		Comparator<String> cmp = new TripleComparator();
		File f = null;
		//If the filename contains sorted, assume the file is sorted and skip the next step
		if (!(new File(hundredFile).getName().contains("sorted"))) {
			
			log.info("Sorting dataset ");
			log.info("If your file is already sorted please change the name of the file to $filename_sorted.$end");
		
			//File name is an UUID
			f = new File(DataProducer.SORTED_FILE);
			try {
				//Try to create the file
				f.createNewFile();
				//Sort the current file and write the output to the sorted filename
				ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(
						new File(fileName), cmp, false), f, cmp);
			} catch (IOException e) {
				log.severe("Couldn't sort file "+fileName+" into file "+f.getName()+" due to:");
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
				return null;
			}
		} else {
			log.info("Assuming file is already sorted as filename contains \"sorted\"");
			//Setting sorted file to hundredfile
			f = new File(hundredFile);
		}
		//For every whished dataset 
		for (int i = 0; i < Config.datasetPercantage.size(); i++) {
			//if the percantage is 100% simply add the fileName to return
			if (Config.datasetPercantage.get(i) == 1.0) {
				log.info("Current Precantage is 100%. Add "+fileName+" to the returning files");
				ret[i] = fileName;
				continue;
			}
			//Get Percantage
			Double per = Config.datasetPercantage.get(i) * 100.0;
			//outputFile will be 
			String outputFile = "datasets" + File.separator + "ds_" + per
					+ ".nt";
			log.info("Current Percantage is "+per+", generate file "+outputFile);
			
			if (per < 100) {
				//Percantage is smaller than 100
				fileName = f.getAbsolutePath();
				//Generate files with DataGenerator
				log.info("Generating smaller file with DataGenerator");
				DataGenerator.generateData(con, Config.graphURI,
						fileName, outputFile, Config.randomFunction,
						Config.datasetPercantage.get(i),
						Double.valueOf(Config.coherenceRoh),
						Double.valueOf(Config.coherenceCh));
			} else {
				//Generate a bigger file
				log.info("Generating bigger file with ExtendedDatasetGenerator");
				ExtendedDatasetGenerator.generatedExtDataset(fileName,
						outputFile, Config.datasetPercantage.get(i));
			}
			log.info("Written " + Config.datasetPercantage.get(i) * 100 + "% Dataset to File");
		}
		//Cache files
		log.info("Cache files for later suites");
		map.put(hundredFile+""+Config.datasetPercantage.hashCode(), ret);
		return ret;

	}

}
