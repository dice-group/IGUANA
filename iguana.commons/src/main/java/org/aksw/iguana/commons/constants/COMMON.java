package org.aksw.iguana.commons.constants;

/**
 * Constants several modules need
 * 
 * @author f.conrads
 *
 */
public class COMMON {

	/*
	 * COMMON CONSTANTS
	 */
	

	/**
	 * The key for the experiment task ID in the properties received from the core
	 */
	public static final String EXPERIMENT_TASK_ID_KEY = "taskID";

	/**
	 * The key for the experiment ID in the properties received from the core
	 */
	public static final String EXPERIMENT_ID_KEY = "expID";

	/**
	 * The key for suite ID in the properties received from the core
	 */
	public static final String SUITE_ID_KEY = "suiteID";

	
	/**
	 * The key for starting an experiment task. Must be in the receiving properties
	 */
	public static final String RECEIVE_DATA_START_KEY = "startExperimentTask";

	/**
	 * The key for ending an experiment task. Must be in the receiving properties
	 */
	public static final String RECEIVE_DATA_END_KEY = "endExperimentTask";


	/**
	 * Key in the properties receiving from the core to start an experiment
	 * as well as internal rp metrics key
	 */
	public static final String METRICS_PROPERTIES_KEY = "metrics";



	/**
	 * TP2RP query time key
	 */
	public static final String RECEIVE_DATA_TIME = "resultTime";

	/**
	 * TP2RP (Controller2RP) query success key
	 */
	public static final String RECEIVE_DATA_SUCCESS = "resultSuccess";

	/**
	 * The number of Queries in the particular experiment
	 * will be used in the meta data.
	 */
	public static final String NO_OF_QUERIES = "noOfQueries";

	public static final String FULL_QUERY_ID_KEY = "query";

	public static final String CONNECTION_ID_KEY = "connID";

	public static final String DATASET_ID_KEY = "datasetID";

	public static final String EXTRA_META_KEY = "extraMeta";

	public static final String EXTRA_IS_RESOURCE_KEY = "setIsResource";

	public static final String QUERY_STRING = "queryString";

	public static final String DOUBLE_RAW_RESULTS = "doubleRawResults";

	public static final String SIMPLE_TRIPLE_KEY = "cleanTripleText";

	public static final String QUERY_STATS = "queryStats";

	public static final Object RECEIVE_DATA_SIZE = "resultSize";

	public static final String QUERY_HASH = "queryHash";

	public static final String WORKER_ID = "workerID";

	/* Various status codes to denote the status of query execution and to prepare QueryExecutionStats object */
	public static final Long QUERY_UNKNOWN_EXCEPTION = 0L;

	public static final Long QUERY_SUCCESS = 1L;

	public static final Long QUERY_SOCKET_TIMEOUT = -1L;

	public static final Long QUERY_HTTP_FAILURE = -2L;

    public static final String EXPERIMENT_TASK_CLASS_ID_KEY = "actualTaskClass" ;

	public static final String BASE_URI = "http://iguana-benchmark.eu";


	public static final String RES_BASE_URI = BASE_URI+"/resource/";
	public static final String PROP_BASE_URI = BASE_URI+"/properties/";
	public static final String CLASS_BASE_URI = BASE_URI+"/class/";
	public static final String METRIC_BASE_URI = CLASS_BASE_URI+"metric/";
	public static final String PENALTY = "penalty";
}
