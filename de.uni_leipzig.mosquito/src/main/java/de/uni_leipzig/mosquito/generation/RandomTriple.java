package de.uni_leipzig.mosquito.generation;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import de.uni_leipzig.mosquito.utils.FileHandler;
import de.uni_leipzig.mosquito.utils.TripleStoreStatistics;

import org.apache.log4j.Logger;
import org.bio_gene.wookie.connection.Connection;
import org.lexicon.jdbc4sparql.SPARQLConstructResultSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

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
    private static Logger logger = Logger.getLogger(RandomTriple.class);
    private static long numberOfGeneratedTriples  = 0;

    private static Model outputModel;
	private static String graphURI=null;
	private static String outputFormat;
	private static String outputFileName = UUID.randomUUID().toString();

	public static void init(String outputFile){
    	numberOfGeneratedTriples = FileHandler.getLineCount(outputFile);
    }
	
	public void setGraphURI(String graphURI){
    	RandomTriple.graphURI = graphURI;
    }
	
	public void setOutputFileName(String outputFileName){
		RandomTriple.outputFileName = outputFileName;
	}
	
	public void setOutputFormat(String outputFormat){
    	RandomTriple.outputFormat = outputFormat;
    }
	
    public static long getNumberOfTriples(){
        return numberOfGeneratedTriples;
    }

    /**
     * Generates a new random triple and stores it into the model, by selecting a random offset each time it is called
     */
    public static void generateTriple(Connection con){
        Model model = null;
        Random generator = new Random();
        try{
            int randomOffset = generator.nextInt(TripleStoreStatistics.tripleCount(con, graphURI).intValue());

            String query = "CONSTRUCT {?s ?p ?o} "+
        			graphURI==null?"":"FROM "+graphURI+
        					" WHERE { {?s ?p ?o}. LIMIT 1000 OFFSET " + randomOffset;

            model = ((SPARQLConstructResultSet)con.execute(query)).getModel();
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
            logger.error("Random triple cannot be extracted due to " + exp.getMessage(), exp);
        }
    }

    /**
     * Reads random triples from and N-triples file
     * The idea is that we read 10 triple from the file and then select some of them at random according to the
     * percentage of instances that should be generated e.g. 10% we should select 1 out of 10, 30% we should select
     * 3 out of 10 and so forth
     * @param   filename    The name of the file that will be used to get triples from
     */
    public static void readTriplesFromFile(String filename, Double precent){
        int numberOfTriplesToSelectFromGroup = (int)(precent*100.0 /10);

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
            fWriter = new FileOutputStream(outputFileName, true);
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
            logger.error("Triple cannot be read from the triples file, due to " + exp.getMessage(), exp);
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