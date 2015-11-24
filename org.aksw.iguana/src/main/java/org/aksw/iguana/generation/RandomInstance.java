package org.aksw.iguana.generation;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;

import org.aksw.iguana.utils.FileHandler;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;


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

    /** The logger. */
    private static Logger logger = Logger.getLogger(RandomInstance.class.getName());

    /** The number of generated triples. */
    private static long numberOfGeneratedTriples = 0;

    /** The graph uri. */
    private static String graphURI=null;
    
    /** The output format. */
    private static String outputFormat;

	/** The output file. */
	private static String outputFile;
	
	static {
		LogHandler.initLogFileHandler(logger, RandomInstance.class.getSimpleName());
	}
    
    /**
     * Sets the graph uri.
     *
     * @param graphURI the new graph uri
     */
    public static void setGraphURI(String graphURI){
    	RandomInstance.graphURI = graphURI;
    }
    
    /**
     * Sets the output format.
     *
     * @param outputFormat the new output format
     */
    public static void setOutputFormat(String outputFormat){
    	RandomInstance.outputFormat = outputFormat;
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
     * Initialize
     *
     * @param outputFile the output file
     */
    public static void init(String outputFile){
    	numberOfGeneratedTriples = FileHandler.getLineCount(outputFile);
    }

    /**
     * Generate triple for instance.
     *
     * @param con Connection to use
     * @param instance the instance
     */
    public static void generateTripleForInstance(Connection con, RDFNode instance){
        Model outputModel = null;
//        QueryEngineHTTP queryExecuter = null;
        String q = "CONSTRUCT {?s ?p ?o} ";
        q+= graphURI==null?"":"FROM <"+graphURI+">";
    	q+=	" WHERE { {?s ?p ?o}. " +
                "FILTER (?s = <%s>)}";
        String query = String.format(q, instance.toString());
        //We should place it in a loop in order to try again and again till the server responds
//        _query = " CONSTRUCT {?s ?p ?o} FROM <http://dbpedia.org>  WHERE { {?s ?p ?o}. FILTER (?s = <http://dbpedia.org/resource/Mahela_Jayawardene>)}";
        while(true){
//            queryExecuter = new QueryEngineHTTP(BenchmarkConfigReader.sparqlEndpoint, query);
            try{
//                outputModel =  queryExecuter.execConstruct();
        		QueryExecution qe = QueryExecutionFactory.createServiceRequest(con.getEndpoint(), QueryFactory.create(query));

            	
                outputModel = qe.execSelect().getResourceModel();

                //Write the model to output file
                numberOfGeneratedTriples += outputModel.size();
                OutputStream stream = new FileOutputStream(outputFile, true);
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
                logger.severe("Triples for instance " + instance +" cannot be fetched");
                LogHandler.writeStackTrace(logger, exp, Level.SEVERE);
                break;
            }
            catch (Exception exp){
                logger.severe("Query = " + query);
                logger.severe("Triples for instance " + instance +" cannot be fetched");
                LogHandler.writeStackTrace(logger, exp, Level.SEVERE);
                logger.info("Trying to get it again, as the server may be down");
            }

        }
    }

	/**
	 * Sets the output file.
	 *
	 * @param outputFile the new output file
	 */
	public static void setOutputFile(String outputFile) {
		RandomInstance.outputFile = outputFile;
	}
}