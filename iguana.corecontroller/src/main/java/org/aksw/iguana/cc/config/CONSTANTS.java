package org.aksw.iguana.cc.config;

/**
 * Constants used only by the Core controller
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
	 * The key to get the timeLimit parameter. 
	 * be aware that timeLimit can be null.
	 */
	public static final String TIME_LIMIT = "timeLimit";


	public static final String NO_OF_QUERY_MIXES = "noOfQueryMixes";
}
