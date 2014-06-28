package de.uni_leipzig.mosquito.generation;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import de.uni_leipzig.mosquito.utils.FileHandler;

import org.apache.log4j.Logger;
import org.bio_gene.wookie.connection.Connection;
import org.lexicon.jdbc4sparql.SPARQLConstructResultSet;

import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Dec 13, 2010
 * Time: 10:36:17 AM
 * To change this template use File | Settings | File Templates.
 * 
 * adjusted by Felix Conrads
 */
public class RandomInstance {

    private static Logger logger = Logger.getLogger(RandomInstance.class);

    private static long numberOfGeneratedTriples = 0;

    private static String graphURI=null;
    
    private static String outputFormat;
    
    public void setGraphURI(String graphURI){
    	RandomInstance.graphURI = graphURI;
    }
    
    public void setOutputFormat(String outputFormat){
    	RandomInstance.outputFormat = outputFormat;
    }
    
    public static long getNumberOfTriples(){
        return numberOfGeneratedTriples;
    }
    
    public static void init(String outputFile){
    	numberOfGeneratedTriples = FileHandler.getLineCount(outputFile);
    }

    public static void generateTripleForInstance(Connection con, RDFNode instance){
        Model outputModel = null;
//        QueryEngineHTTP queryExecuter = null;

        String query = String.format("CONSTRUCT {?s ?p ?o} "+
        			graphURI==null?"":"FROM "+graphURI+
        					" WHERE { {?s ?p ?o}. " +
                    "FILTER (?s = <%s>)}", instance.toString());
        //We should place it in a loop in order to try again and again till the server responds
//        _query = " CONSTRUCT {?s ?p ?o} FROM <http://dbpedia.org>  WHERE { {?s ?p ?o}. FILTER (?s = <http://dbpedia.org/resource/Mahela_Jayawardene>)}";
        while(true){
//            queryExecuter = new QueryEngineHTTP(BenchmarkConfigReader.sparqlEndpoint, query);
            try{
//                outputModel =  queryExecuter.execConstruct();
                outputModel = ((SPARQLConstructResultSet)con.execute(query)).getModel();


                //Write the model to output file
                numberOfGeneratedTriples += outputModel.size();
                OutputStream stream = new FileOutputStream(outputFormat, true);
                try{
                    outputModel.write(stream , outputFormat.toUpperCase());
                }
                finally {
                    stream.flush();
                    stream.close();
                }

                logger.info("Triples for instance " + instance +" are successfully written to file");
                break;
            }
            catch(com.hp.hpl.jena.shared.JenaException exp){
                logger.error("Triples for instance " + instance +" cannot be fetched", exp);
                break;
            }
            catch (Exception exp){
                logger.error("Query = " + query);
                logger.error("Triples for instance " + instance +" cannot be fetched", exp);
                logger.info("Trying to get it again, as the server may be down");
            }

        }
    }
}