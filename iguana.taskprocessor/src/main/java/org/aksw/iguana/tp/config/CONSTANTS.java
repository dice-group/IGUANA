package org.aksw.iguana.tp.config;

/**
 * Constants used only by TaskProcessor
 * 
 * @author f.conrads
 *
 */
public class CONSTANTS {

    /**
     * The key to set the workerID in the Extra Meta properties
     * and the properties name in the final results to get the workerID
     */
    public static final String WORKER_ID_KEY = "workerID";

    /**
     * The key to set the workerType in the Extra Meta properties
     * and the properties name in the final results to get the workerType
     */
    public static final String WORKER_TYPE_KEY = "workerType";

    /**
     * The key to set/get the base value for the gaussian intervall 
     * which will be used to randomly select a time to wait before each query
     */
    public static final String GAUSSIAN_LATENCY = "gaussianLatency";

    /**
     * The key to set/get a fixed value to wait before each query
     */
    public static final String FIXED_LATENCY = "fixedLatency";

    /**
     * The key to get/set the current sparql endpoint to test
     */
    public static final String SPARQL_CURRENT_ENDPOINT = "sparqlEndpoint";

    /**
     * The key to get/set the timeout for sparql queries
     * 0 and less means no timeout,
     * default is 180s
     */
    public static final String SPARQL_TIMEOUT = "sparqlTimeOut";

    /**
     * 
     */
    public static final String QUERY_FILE_LIST = "queryFileList";


}
