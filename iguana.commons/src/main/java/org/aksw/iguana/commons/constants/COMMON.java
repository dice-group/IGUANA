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
	 * The property key for the host where all the rabbit messages will be consumed from
	 */
	public static final String CONSUMER_HOST_KEY = "iguana.consumer.host";
	
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



	public static final String QUERY_ID_KEY = "queryID";

	public static final String CONNECTION_ID_KEY = "connID";

	public static final String DATASET_ID_KEY = "datasetID";

	public static final String EXTRA_META_KEY = "extraMeta";

	public static final String EXTRA_IS_RESOURCE_KEY = "setIsResource";

	public static final String IS_EXTRA_META_KEY = "isExtraMeta";
	
	public static final String CLASS_NAME="class";
	
	public static final String CONSTRUCTOR_ARGS="constructorArgs";
	
	/*
	 * QUEUE NAMES
	 */
	

    /**
     * The Queue name for  the rabbitMQ connection from the Iguana core to the ResultPRocessing (here)
     */
	public static final String CORE2RP_QUEUE_NAME = "core2rp";

	public static final String DEFAULT_IGAUNA_RP_PROPERTIES_FILE_NAME = "iguana.properties";

	public static final String MC2TP_QUEUE_NAME = "mc2tp";

	public static final String TP2MC_QUEUE_NAME = "tp2mc";

	public static final String TASK_FINISHED_MESSAGE = "task_finished";

	public static final String CONSTRUCTOR_ARGS_CLASSES = "constructorClasses";

	public static final String GENERATION_FINISHED_MESSAGE = "generation_finished";

	public static final String DG2MC_QUEUE_NAME = "dg2mc";

	public static final String MC2DG_QUEUE_NAME = "mc2dg";

	public static final String DATAGEN_CLASS_NAME = "datagen.class";

	public static final String DATAGEN_CONSTRUCTOR_ARGS = "datagen.constructorArgs";

	public static final String DATAGEN_CONSTRUCTOR_ARGS_CLASSES = "datagen.constructorClasses";

	public static final String LOADER_CLASS_NAME = "loader.class";

	public static final String LOADER_CONSTRUCTOR_ARGS = "loader.constructorArgs";

	public static final String LOADER_CONSTRUCTOR_ARGS_CLASSES = "loader.constructorClasses";
	
}
