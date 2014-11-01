package de.uni_leipzig.iguana.generation;

import com.hp.hpl.jena.rdf.model.RDFNode;

import de.uni_leipzig.iguana.data.Selector;
import de.uni_leipzig.iguana.utils.FileHandler;
import de.uni_leipzig.iguana.utils.TripleStoreStatistics;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Dec 19, 2010
 * Time: 5:52:25 PM
 * Data generator for DBpedia datasets of different sizes
 * 
 * adjusted by Felix Conrads
 */
public class DataGenerator {

    /** The log. */
    private static Logger log = Logger.getLogger(DataGenerator.class.getName());
    
    /**
     * Generates the data using the generation method set in the configuration file.
     *
     * @param con Connection to use
     * @param graphURI graphURI to use for the Connection (can be null)
     * @param inputFile the input file
     * @param outputFile the output file
     * @param method the method to use for generation (RandomInstance, RandomTriples, coherence)
     * @param percent the percentage to reach
     * @param roh the relaxtion parameter if the method coherence should be used
     * @param coherence the coherence to reach if method coherence should be used
     */
    public static void generateData(Connection con, String graphURI, String inputFile, String outputFile, String method, Double percent, Double roh, Double coherence) {
    	
    	
        long numberOfTriplesToBeGenerated = (int)(TripleStoreStatistics.tripleCount(con, graphURI) * percent);
        RandomInstance.setGraphURI(graphURI);
        RandomInstance.setOutputFormat("N-TRIPLES");
        RandomInstance.setOutputFile(outputFile);
        RandomTriple.setGraphURI(graphURI);
        RandomTriple.setOutputFormat("N-TRIPLES");
        RandomTriple.setOutputFileName(outputFile);

        if(method.compareTo("RandomInstance") == 0){
            //List of visited nodes, in order not process the same nodes more than once 
            Collection<String> visitedNodesList = FileHandler.getSubjectsInFile(outputFile);
//            ArrayList<RDFNode> visitedNodesList = new ArrayList<RDFNode> ();
            log.info("NUMBER OF VISITED NODES = " + visitedNodesList.size());

//            InstanceSelector selector = new InstanceSelector();
//            selector.loadClassInstancesFromFile(BenchmarkConfigReader.classesInputFilename);

            Selector selector = new Selector(con, graphURI);
           
            //This variable is used as an indicator to indicate whether the last node was processed successfully
            //so we can go to another one or we should try with same node again
            boolean lastNodeProcessedSuccessfully = true;

            RDFNode node = selector.getRandomInstance();
            
            while((node != null) && (FileHandler.getLineCount(outputFile) < numberOfTriplesToBeGenerated)){
                try{
                    if(lastNodeProcessedSuccessfully){
                        log.info("# of Triples written = " + FileHandler.getLineCount(outputFile));
                        //Keep selecting a random node until we encounter an unvisited node
                        boolean firstTime = true;
                        do{
                            if(firstTime){
                                firstTime = false;
                            }
                            else{
                                log.info("INSTANCE IS ALREADY VISITED BEFORE, SO WE SHOULD SELECT ANOTHER ONE");
                            }

                            node = selector.getRandomInstance();
                            //logger.info("NODE = " + node.toString());
                        }while(visitedNodesList.contains(node.toString()));

                    }
//                    ModelUtilities.generateTripleForInstance(node);
                    RandomInstance.generateTripleForInstance(con, node);
                    lastNodeProcessedSuccessfully = true;
                }
                catch(Exception exp){
                	log.severe("Error processing node titled = " + node);
                	LogHandler.writeStackTrace(log, exp, Level.SEVERE);
                    lastNodeProcessedSuccessfully = false;
                }
            }
        }
        else if(method.compareTo("RandomTriple") == 0){
            /*while(RandomTriple.getNumberOfTriples() < numberOfTriplesToBeGenerated){
                RandomTriple.generateTriple();
            } */
        	//TODO Decompression!
//            if(BenchmarkConfigReader.decompressFiles)
//                FileUtilities.decompressAllFiles();
//            FileUtilities.iterateThroughFiles(new String[]{"nt"});

        	
//            for(String fileName : new File(new File(inputFile).getAbsolutePath()).list(new FilenameFilter(){
//
//				@Override
//				public boolean accept(File arg0, String arg1) {
//					String lwname = arg1.toLowerCase();
//					if (lwname.matches(".*\\.(nt|n3|rdf/xml|ttl)")) {
//						return true;
//					}
//					return false;
//				}
//            	
//            })){
                
                RandomTriple.readTriplesFromFile(inputFile, percent);
//            }
//            RandomTriple.readTriplesFromFile();
        }
        else if(method.compareTo("coherence")==0){
        	DataProducer.setRoh(roh);
        	DataProducer.writeData(inputFile, outputFile, graphURI, percent, coherence);
        }
        else{
            log.severe("Unknown extraction method, program should terminate");
            System.exit(1);
        }

        /*ModelUtilities.readTotalDBpediaTriples();*/
    }

}