package de.uni_leipzig.iguana.benchmark.processor;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

import de.uni_leipzig.iguana.clustering.ExternalSort;
import de.uni_leipzig.iguana.data.TripleStoreHandler;
import de.uni_leipzig.iguana.generation.DataGenerator;
import de.uni_leipzig.iguana.generation.DataProducer;
import de.uni_leipzig.iguana.generation.ExtendedDatasetGenerator;
import de.uni_leipzig.iguana.utils.Config;
import de.uni_leipzig.iguana.utils.FileHandler;
import de.uni_leipzig.iguana.utils.comparator.TripleComparator;

public class DatasetGeneratorProcessor {

	
	private static Logger log = Logger.getLogger(DatasetGeneratorProcessor.class
			.getSimpleName());
	private static Map<String, String[]> map = new HashMap<String, String[]>();

	static {
		LogHandler
				.initLogFileHandler(log, DatasetGeneratorProcessor.class.getSimpleName());
	}
	
	
	public static String[] getDatasetFiles(Connection con, String hundredFile) {
		if(map.containsKey(hundredFile+""+Config.datasetPercantage.hashCode())){
			return map .get(hundredFile+""+Config.datasetPercantage.hashCode());
		}
		String[] ret = new String[Config.datasetPercantage.size()];
		new File("datasets" + File.separator).mkdir();
		String fileName = hundredFile;
		if (hundredFile == null || !(new File(hundredFile).exists())
				|| new File(hundredFile).isDirectory()) {

			fileName = "datasets" + File.separator + "ds_100.nt";
			if (new File(hundredFile).isDirectory()) {
				try {
					FileHandler.writeFilesToFile(hundredFile, fileName);
				} catch (IOException e) {
					LogHandler.writeStackTrace(log, e, Level.SEVERE);
					return null;
				}
			} else {
				log.info("Writing 100% Dataset to File");
				TripleStoreHandler.writeDatasetToFile(con,
						Config.graphURI, fileName);
			}
		}
		Comparator<String> cmp = new TripleComparator();
		File f = null;
		if (!(new File(hundredFile).getName().contains("sorted"))) {
			log.info("Sorting data");
			f = new File(DataProducer.SORTED_FILE);
			try {
				f.createNewFile();
				ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(
						new File(fileName), cmp, false), f, cmp);
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
				return null;
			}
		} else {
			log.info("File is sorted");
			f = new File(hundredFile);
		}
		for (int i = 0; i < Config.datasetPercantage.size(); i++) {
			if (Config.datasetPercantage.get(i) == 1.0) {
				ret[i] = fileName;
				continue;
			}
			Double per = Config.datasetPercantage.get(i) * 100.0;
			String outputFile = "datasets" + File.separator + "ds_" + per
					+ ".nt";
			if (per < 100) {
				fileName = f.getAbsolutePath();
				DataGenerator.generateData(con, Config.graphURI,
						fileName, outputFile, Config.randomFunction,
						Config.datasetPercantage.get(i),
						Double.valueOf(Config.coherenceRoh),
						Double.valueOf(Config.coherenceCh));
			} else {
				ExtendedDatasetGenerator.generatedExtDataset(fileName,
						outputFile, Config.datasetPercantage.get(i));
			}
			log.info("Writing " + Config.datasetPercantage.get(i) * 100 + "% Dataset to File");
		}
		map.put(hundredFile+""+Config.datasetPercantage.hashCode(), ret);
		return ret;

	}

}
