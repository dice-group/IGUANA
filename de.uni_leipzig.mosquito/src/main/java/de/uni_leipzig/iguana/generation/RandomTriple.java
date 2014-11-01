package de.uni_leipzig.iguana.generation;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.uni_leipzig.iguana.utils.FileHandler;
import de.uni_leipzig.iguana.utils.TripleStoreStatistics;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;
import org.lexicon.jdbc4sparql.SPARQLConstructResultSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Sep 29, 2010
 * Time: 4:32:35 PM
 * This class represents the second methodology of selecting triple, i.e. selecting triples in totally random fashion   
 * 
 * adjusted by Felix Conrads
 */
public class RandomTriple {
    
    /** The logger. */
    private static Logger logger = Logger.getLogger(RandomTriple.class.getSimpleName());
    
    /** The number of generated triples. */
    private static long numberOfGeneratedTriples  = 0;

    /** The output model. */
    private static Model outputModel;
	
	/** The graph uri. */
	private static String graphURI=null;
	
	/** The output format. */
	private static String outputFormat;
	
	/** The output file name. */
	private static String outputFileName = UUID.randomUUID().toString();

	static {
		LogHandler.initLogFileHandler(logger, RandomInstance.class.getSimpleName());
	}
	
	/**
	 * Initialization
	 *
	 * @param outputFile the output file
	 */
	public static void init(String outputFile){
    	numberOfGeneratedTriples = FileHandler.getLineCount(outputFile);
    }
	
	/**
	 * Sets the graph uri.
	 *
	 * @param graphURI the new graph uri
	 */
	public static void setGraphURI(String graphURI){
    	RandomTriple.graphURI = graphURI;
    }
	
	/**
	 * Sets the output file name.
	 *
	 * @param outputFileName the new output file name
	 */
	public static void setOutputFileName(String outputFileName){
		RandomTriple.outputFileName = outputFileName;
	}
	
	/**
	 * Sets the output format.
	 *
	 * @param outputFormat the new output format
	 */
	public static void setOutputFormat(String outputFormat){
    	RandomTriple.outputFormat = outputFormat;
    }
	
    /**
     * Gets the number of triples.
     *
     * @return the number of triples
     */
    public static long getNumberOfTriples(){
        return numberOfGeneratedTriples;
    }

    /**
     * Generates a new random triple and stores it into the model, by selecting a random offset each time it is called.
     *
     * @param con Connection to use
     */
    public static void generateTriple(Connection con){
        Model model = null;
        Random generator = new Random();
        try{
            int randomOffset = generator.nextInt(TripleStoreStatistics.tripleCount(con, graphURI).intValue());

            String query = "CONSTRUCT {?s ?p ?o} ";
            query+=	graphURI==null?"":"FROM <"+graphURI+">";
        	query+=" WHERE { {?s ?p ?o}. LIMIT 1000 OFFSET " + randomOffset;
        	SPARQLConstructResultSet res = (SPARQLConstructResultSet)con.execute(query);
            model = res.getModel();
            res.getStatement().close();
//            queryExecuter = new QueryEngineHTTP(BenchmarkConfigReader.sparqlEndpoint, query);
//            model =  queryExecuter.execConstruct();

            if(outputModel == null)
                outputModel = model;
            else
                outputModel.add(model);

            //We select 1000 triples at once, and then randomly select 100 items out of them to work on, as
            //it is very slow to select just 1 triple each time we call SPARQL
            ArrayList<Statement> stmtList = new ArrayList<Statement>((int)model.size());
            StmtIterator iteratorForStatements = model.listStatements();

            int i = 0;
            while(iteratorForStatements.hasNext())
                stmtList.add(iteratorForStatements.next());

            model.removeAll();
            for(i=0; i<100; i++){
                int randomIndex = generator.nextInt(stmtList.size());
                model.add(stmtList.remove(randomIndex));
            }

            System.out.println("Number of triples that exist in file = " + numberOfGeneratedTriples);
            numberOfGeneratedTriples += 100;

            //We should wait to have e.g. 1000 instances and then write them at once, to minimize disk usage, i.e. not to
            //write instance by instance
//            if(outputModel.size() >= 1000){
//                ModelUtilities.writeModel(outputModel);
//                outputModel = null;
//            }
            
            model.write(new FileOutputStream(outputFileName), outputFormat);
        }
        catch(Exception exp){
            logger.severe("Random triple cannot be extracted due to " + exp.getMessage());
            LogHandler.writeStackTrace(logger, exp, Level.SEVERE);
        }
    }

    /**
     * Reads random triples from and N-triples file
     * The idea is that we read 10 triple from the file and then select some of them at random according to the
     * percentage of instances that should be generated e.g. 10% we should select 1 out of 10, 30% we should select
     * 3 out of 10 and so forth
     *
     * @param filename the filename
     * @param percent the percentage to genrate
     */
    public static void readTriplesFromFile(String filename, Double percent){
        int numberOfTriplesToSelectFromGroup = (int)(percent*100.0 /10);
        File file = null;
        FileReader fReader = null;
        Random generator = new Random();
        FileOutputStream fWriter = null;

        int lineIndex = 0;
        ArrayList <String>lines = new ArrayList<String>(10);
        String line = "";

        try{
            //Input file
            file = new File(filename);
            fReader = new FileReader(file);
            LineNumberReader lnReader = new LineNumberReader(fReader);

            //Output file
            fWriter = new FileOutputStream(outputFileName);
            int numberOfTriplesExtracted = 0;
            logger.info("Extraction of triples from file " + filename + " has started");
            while ((line = lnReader.readLine()) != null){
                lines.add(line);
                lineIndex++;

                if(lineIndex == 10){
                    for(int i = 0; i < numberOfTriplesToSelectFromGroup && lines.size() > 0; i++){
                        int randomInstanceIndex = generator.nextInt(lines.size());
                        logger.info("Lines size = " + lines.size());
                        String triple = lines.remove(randomInstanceIndex);
                        triple += "\n";
                        fWriter.write(triple.getBytes());
                        numberOfTriplesExtracted ++;
                    }
                    lines.clear();
                }

                lineIndex = lineIndex % 10;
            }
            //If any triples are still remaining in the array, we should write them, in case we want 100% extraction
            if(lines.size()>0 && numberOfTriplesToSelectFromGroup==10){
                for(String triple:lines){
                    fWriter.write(triple.getBytes());
                }
            }


            if(fReader != null)
                fReader.close();

            if(fWriter != null)
                fReader.close();
            logger.info(numberOfTriplesExtracted + " triples are successfully extracted at random from file " + filename);

        }
        catch(Exception exp){
            logger.severe("Triple cannot be read from the triples file, due to " + exp.getMessage());
            LogHandler.writeStackTrace(logger, exp, Level.SEVERE);
        }
//        finally{
//            if(fReader != null)
//                fReader.close();
//
//            if(fWriter != null)
//                fReader.close();
//        }
    }


}