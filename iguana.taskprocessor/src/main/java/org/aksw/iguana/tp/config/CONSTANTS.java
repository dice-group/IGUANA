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
     * The key to set/get the simulated Network Latency Strategy 
     */
    public static final String LATENCY_STRATEGY = "latencyStrategy";

    /**
     * The key to set/get the base value corresponding to the latency strategy
     */
    public static final String LATENCY_BASE_VALUE = "latencyBaseValue";


}
