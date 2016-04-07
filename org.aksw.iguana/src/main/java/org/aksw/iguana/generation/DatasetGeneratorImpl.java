package org.aksw.iguana.generation;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.benchmark.processor.DatasetGeneratorProcessor;
import org.aksw.iguana.data.TripleStoreHandler;
import org.aksw.iguana.utils.Config;
import org.aksw.iguana.utils.ExternalSort;
import org.aksw.iguana.utils.FileHandler;
import org.aksw.iguana.utils.comparator.TripleComparator;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

public class DatasetGeneratorImpl implements DatasetGenerator {

	private static Logger log = Logger.getLogger(DatasetGeneratorImpl.class
			.getSimpleName());

	static {
		LogHandler
				.initLogFileHandler(log, DatasetGeneratorProcessor.class.getSimpleName());
	}
	
	@Override
	public boolean generateDataset(Connection con, String initialFile,
			double percent, String outputFile) {
				

				String fileName = initialFile;
				//Test if hundredFile doesn't exists is not set or is a directory
				if (initialFile == null || !(new File(initialFile).exists())
						|| new File(initialFile).isDirectory()) {

					//Init file name of creating one file is: datasets/ds_100.nt
					fileName = "datasets" + File.separator + "ds_100.nt";
					if (new File(initialFile).isDirectory()) {
						log.info("Initial 100% file is a directory");
						log.info("Try to write all files in the directory together");
						try {
							//Tries to write alle files in the directory together
							FileHandler.writeFilesToFile(initialFile, fileName);
						} catch (IOException e) {
							log.severe("Couldn't write all files together due to: ");
							LogHandler.writeStackTrace(log, e, Level.SEVERE);
							return false;
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
				if (!(new File(initialFile).getName().contains("sorted"))) {
					
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
						return false;
					}
				} else {
					log.info("Assuming file is already sorted as filename contains \"sorted\"");
					//Setting sorted file to hundredfile
					f = new File(initialFile);
				}
								
					if (percent < 1.0) {
						//Percantage is smaller than 100
						fileName = f.getAbsolutePath();
						//Generate files with DataGenerator
						log.info("Generating smaller file with DataGenerator");
						DataGenerator.generateData(con, Config.graphURI,
								fileName, outputFile, Config.randomFunction,
								percent,
								Double.valueOf(Config.coherenceRoh),
								Double.valueOf(Config.coherenceCh));
					} else {
						//Generate a bigger file
						log.info("Generating bigger file with ExtendedDatasetGenerator");
						ExtendedDatasetGenerator.generatedExtDataset(fileName,
								outputFile, percent);
					}
				return true;
	}

	@Override
	public void setProperties(Properties p) {
		// TODO Auto-generated method stub

	}


}
