/**
 * 
 */
package org.aksw.iguana.cc.tasks.impl;

import org.aksw.iguana.cc.config.CONSTANTS;
import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.query.QueryHandler;
import org.aksw.iguana.cc.query.QueryHandlerFactory;
import org.aksw.iguana.cc.query.impl.InstancesQueryHandler;
import org.aksw.iguana.cc.tasks.AbstractTask;
import org.aksw.iguana.cc.worker.Worker;
import org.aksw.iguana.cc.worker.WorkerFactory;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;


/**
 * Stresstest.
 * Will stresstest a connection using several Workers (simulated Users) each in one thread.
 */
@Shorthand("Stresstest")
public class Stresstest extends AbstractTask {


	private static final Logger LOGGER = LoggerFactory
			.getLogger(Stresstest.class);

	private ArrayList workerConfig;
	private LinkedHashMap warmupConfig;

	private Double timeLimit;
	private Long noOfQueryMixes;
	protected List<Worker> workers = new LinkedList<Worker>();
	private Instant startTime;
	private String qhClassName;
	private Long noOfWorkers= 0L;
	protected  String qhCacheFolder = "queryInstances";

	private Double warmupTimeMS;



	private HashMap<Object, Object> qhConfig;
	private List<Worker> warmupWorkers = new ArrayList<>();
	private HashMap warmupQHConfig;
	private String warmupQHClass;


	public Stresstest(Integer timeLimit, ArrayList workers, LinkedHashMap queryHandler) throws FileNotFoundException {
		this(timeLimit, workers, queryHandler, null);
	}

	public Stresstest(Integer timeLimit, ArrayList workers, LinkedHashMap queryHandler, LinkedHashMap warmup) throws FileNotFoundException {
		this.timeLimit=timeLimit.doubleValue();
		this.workerConfig = workers;
		this.qhConfig = queryHandler;
		this.warmupConfig = warmup;
	}

	public Stresstest(ArrayList workers, LinkedHashMap queryHandler, Integer noOfQueryMixes) throws FileNotFoundException {
		this(workers, queryHandler, null, noOfQueryMixes);
	}

	public Stresstest(ArrayList workers, LinkedHashMap queryHandler, LinkedHashMap warmup, Integer noOfQueryMixes) throws FileNotFoundException {
		this.noOfQueryMixes=noOfQueryMixes.longValue();
		this.workerConfig = workers;
		this.qhConfig = queryHandler;
		this.warmupConfig = warmup;
	}

	private void setConfig(ArrayList<HashMap> workers, HashMap queryHandler, HashMap warmup){

		noOfWorkers+=createWorkers(workers, this.workers, this.timeLimit);
		//let TypedFactory create queryHandlerConfiguration from className and configuration and add Workers
		this.qhClassName = queryHandler.get("className").toString();
		this.qhConfig = (HashMap)queryHandler.getOrDefault("configuration", new HashMap<>());
		qhConfig.put("workers", this.workers);

		//If warmup
		if(warmup!=null){
			//set time
			this.warmupTimeMS = ((Integer) warmup.get("timeLimit")).doubleValue();
			//set warmup workers
			ArrayList<HashMap> warmupWorkerConfig = (ArrayList<HashMap>) warmup.get("workers");
			createWorkers(warmupWorkerConfig, this.warmupWorkers, this.warmupTimeMS);
			//if warmup uses a different queryHandler than the actual one create the query handler
			createWarmupQueryHandler(warmup);
		}
		addMetaData();
	}

	private void createWarmupQueryHandler(HashMap warmup) {
		if(warmup.containsKey("queryHandler")){
			HashMap warmupQueryHandler = (HashMap) warmup.get("queryHandler");
			this.warmupQHClass = warmupQueryHandler.get("className").toString();
			this.warmupQHConfig = (HashMap)warmupQueryHandler.getOrDefault("configuration", new HashMap<>());
			this.warmupQHConfig.put("workers", this.warmupWorkers);
		}else{
			//otherwise use default
			this.warmupQHClass = qhClassName;
			//create copy of the current configuration
			this.warmupQHConfig = new HashMap(qhConfig);
			this.warmupQHConfig.put("workers", this.warmupWorkers);
		}
	}

	private int createWorkers(ArrayList<HashMap> workers, List<Worker> workersToAddTo, Double timeLimit){
		int noOfWorkers=0;
		for(HashMap workerConfig : workers){
			noOfWorkers += createWorker(workerConfig, workersToAddTo, timeLimit, noOfWorkers);
		}
		return noOfWorkers;
	}

	private int createWorker(HashMap workerConfig, List<Worker> workersToAddTo, Double timeLimit, Integer baseID) {
		//let TypedFactory create from className and configuration
		String className = workerConfig.remove("className").toString();
		//if shorthand classname is used, exchange to full classname
		Integer threads = (Integer)workerConfig.remove("threads");
		workerConfig.put("connection", con);
		workerConfig.put("taskID", taskID);
		if(timeLimit!=null)
			workerConfig.put("timeLimit", timeLimit.intValue());
		for(int i=0;i<threads;i++) {
			workerConfig.put("workerID", baseID+i);
			Worker worker = new WorkerFactory().create(className, workerConfig);
			workersToAddTo.add(worker);
		}
		return threads;
	}

	/**
	 * Add extra Meta Data
	 */
	@Override
	public void addMetaData() {
		super.addMetaData();
		Properties extraMeta = new Properties();
		if(timeLimit!=null)
			extraMeta.put(CONSTANTS.TIME_LIMIT, timeLimit);
		if(noOfQueryMixes!=null)
			extraMeta.put(CONSTANTS.NO_OF_QUERY_MIXES, noOfQueryMixes);
		extraMeta.put("noOfWorkers", noOfWorkers);
		this.metaData.put(COMMON.EXTRA_META_KEY, extraMeta);
	}

	@Override
	public void init(String[] ids, String dataset, Connection connection)  {
		super.init(ids, dataset, connection);
		setConfig(workerConfig, qhConfig , warmupConfig);

		// create from construct args and class
		QueryHandlerFactory factory = new QueryHandlerFactory();
		//create query handler and generate queries, set them to the workers
		QueryHandler queryHandler = factory.create(qhClassName, qhConfig);
		if(queryHandler instanceof InstancesQueryHandler){
			((InstancesQueryHandler)queryHandler).setOutputFolder(this.qhCacheFolder);
		}
		queryHandler.generate();

		//init warmup queries if set
		if(warmupQHClass!=null){
			QueryHandler warmupQH = factory.create(warmupQHClass, warmupQHConfig);
			if(warmupQH instanceof InstancesQueryHandler){
				((InstancesQueryHandler)warmupQH).setOutputFolder(this.qhCacheFolder);
			}
			warmupQH.generate();
		}

        Model tripleStats = queryHandler.generateTripleStats(taskID);
		StringWriter sw = new StringWriter();
		RDFDataMgr.write(sw, tripleStats, RDFFormat.NTRIPLES);
		this.metaData.put(COMMON.SIMPLE_TRIPLE_KEY, sw.toString());
		this.metaData.put(COMMON.QUERY_STATS, tripleStats);


	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aksw.iguana.cc.tasks.Task#start()
	 */
	@Override
	public void execute() {
		warmup();
		LOGGER.info("Task with ID {{}} will be executed now", this.taskID);
		// Execute each Worker in ThreadPool
		ExecutorService executor = Executors.newFixedThreadPool(noOfWorkers.intValue());
		this.startTime = Instant.now();
		for (Worker worker : workers) {
			executor.execute(worker);
		}
		LOGGER.info("[TaskID: {{}}]All {{}} workers have been started", taskID, noOfWorkers);
		// wait timeLimit or noOfQueries
		executor.shutdown();
		while (!isFinished()) {
			// check if worker has results yet
			for (Worker worker : workers) {
				// if so send all results buffered
				sendWorkerResult(worker);
			}
			
		}
		LOGGER.debug("Sending stop signal to workers");
		// tell all workers to stop sending properties, thus the await termination will
		// be safe with the results
		for (Worker worker : workers) {
			worker.stopSending();
		}
		// Wait 5seconds so the workers can stop themselves, otherwise they will be
		// stopped
		try {
			LOGGER.debug("Will shutdown now...");

			LOGGER.info("[TaskID: {{}}] Will shutdown and await termination in 5s.", taskID);
			boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);
			LOGGER.info("[TaskID: {{}}] Task completed. Thread finished status {}", taskID, finished);
		} catch (InterruptedException e) {
			LOGGER.error("[TaskID: {{}}] Could not shutdown Threads/Workers due to ...", taskID);
			LOGGER.error("... Exception: ", e);
			try {
				executor.shutdownNow();
			}catch(Exception e1) {
				LOGGER.error("Problems shutting down", e1);
			}

		}
		
	}

	private void sendWorkerResult(Worker worker){
		for (Properties results : worker.popQueryResults()) {
			try {
				// send results via RabbitMQ
				LOGGER.debug("[TaskID: {{}}] Send results", taskID);
				this.sendResults(results);
				LOGGER.debug("[TaskID: {{}}] results could be send", taskID);
			} catch (IOException e) {
				LOGGER.error("[TaskID: {{}}] Could not send results due to exc.",taskID, e);
				LOGGER.error("[TaskID: {{}}] Results: {{}}", taskID, results);
			}
		}
	}

	
	@Override
	public void close() {
		super.close();

	}
	
	protected long warmup() {
		if(warmupTimeMS==null||warmupTimeMS==0l) {
			return 0;
		}
		if(warmupWorkers.size()==0) {
			return 0;
		}
		LOGGER.info("[TaskID: {{}}] will start {{}}ms warmup now using {} no of workers in total.", taskID, warmupTimeMS, warmupWorkers.size());
		return executeWarmup(warmupWorkers);
	}

	
	private long executeWarmup(List<Worker> warmupWorkers) {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		for(Worker worker : warmupWorkers) {
			exec.submit(worker);
		}
		//wait as long as needed
		Instant start = Instant.now();
		exec.shutdown();
		while(durationInMilliseconds(start, Instant.now()) <= warmupTimeMS) {
			//clean up RAM
			for(Worker worker: warmupWorkers) {
				worker.popQueryResults();
			}
			try {
				TimeUnit.MILLISECONDS.sleep(50);
			}catch(Exception e) {
				LOGGER.error("Could not warmup ");
			}
		}
		for(Worker worker : warmupWorkers) {
			worker.stopSending();
		}
		try {
			exec.awaitTermination(5, TimeUnit.SECONDS);

		} catch (InterruptedException e) {
			LOGGER.warn("[TaskID: {{}}] Warmup. Could not await Termination of Workers.", taskID);
		}
		try {
			exec.shutdownNow();
		}catch(Exception e1) {
			LOGGER.error("Shutdown problems ", e1);
		}
		//clear up
		long queriesExec = 0;
		for(Worker w : warmupWorkers){
			queriesExec+=w.getExecutedQueries();
		}
		warmupWorkers.clear();
		LOGGER.info("[TaskID: {{}}] Warmup finished.", taskID);
		return queriesExec;
	}

	/**
	 * Checks if restriction (e.g. timelimit or noOfQueryMixes for each Worker)
	 * occurs
	 * 
	 * @return true if restriction occurs, false otherwise
	 */
	protected boolean isFinished() {
		if (timeLimit !=null) {
			try {
				TimeUnit.MILLISECONDS.sleep(10);
				
			}catch(Exception e) {
				LOGGER.error("Could not warmup ");
			}
			Instant current = Instant.now();
			double passed_time = timeLimit - durationInMilliseconds(this.startTime, current);
			return passed_time <= 0D;
		}
		else if (noOfQueryMixes != null) {
			// use noOfQueries of SPARQLWorkers (as soon as a worker hit the noOfQueries, it
			// will stop sending results
			// UpdateWorker are allowed to execute all their updates
			boolean endFlag=true;
			for (Worker worker : workers) {
				long queriesInMix = 0;

					LOGGER.debug("No of query Mixes: {} , queriesInMix {}", worker.getExecutedQueries(),noOfQueryMixes);
					//Check for each worker, if the
					if (worker.hasExecutedNoOfQueryMixes(noOfQueryMixes)) {
						if(!worker.isTerminated()) {
							//if the worker was not already terminated, send last results, as tehy will not be sended afterwards
							sendWorkerResult(worker);
						}
						worker.stopSending();
					}
					else {
						endFlag = false;
					}

			}
			return endFlag;
		}
		LOGGER.error("Neither time limit nor NoOfQueryMixes is set. executing task now");
		return true;
	}

	public long getExecutedQueries(){
		long ret = 0;
		for(Worker worker: workers){
			ret += worker.getExecutedQueries();
		}
		return ret;
	}

}
