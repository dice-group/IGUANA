/**
 * 
 */
package org.aksw.iguana.cc.tasks.impl.stresstest;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.tasks.impl.stresstest.worker.impl.HttpWorker;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.cc.config.CONSTANTS;
import org.aksw.iguana.cc.query.QueryHandler;
import org.aksw.iguana.cc.query.QueryHandlerFactory;
import org.aksw.iguana.cc.query.impl.InstancesQueryHandler;
import org.aksw.iguana.cc.tasks.AbstractTask;
import org.aksw.iguana.cc.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.cc.tasks.impl.stresstest.worker.Worker;
import org.aksw.iguana.cc.tasks.impl.stresstest.worker.WorkerFactory;
import org.aksw.iguana.cc.tasks.impl.stresstest.worker.impl.SPARQLWorker;
import org.aksw.iguana.cc.tasks.impl.stresstest.worker.impl.UPDATEWorker;
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



@Shorthand("Stresstest")
public class Stresstest extends AbstractTask {


	private static final Logger LOGGER = LoggerFactory
			.getLogger(Stresstest.class);

	private ArrayList workerConfig;
	private LinkedHashMap warmupConfig;

	private Double timeLimit;
	private Long noOfQueryMixes;
	private List<Worker> workers = new LinkedList<Worker>();
	private Instant startTime;
	private String qhClassName;
	private String[] qhConstructorArgs;
	private Long noOfWorkers= 0L;

	private Double warmupTimeMS;
	private String warmupQueries;
	private String warmupUpdates;


	protected String baseUri = "http://iguana-benchmark.eu";
	private String iguanaResource = baseUri + "/resource/";
	private String iguanaProperty = baseUri + "/properties/";

	private HashMap<Object, Object> qhConfig;


	//@ParameterNames(names={"timeLimit", "workers", "queryHandler"})
	public Stresstest(Integer timeLimit, ArrayList workers, LinkedHashMap queryHandler) throws FileNotFoundException {
		this(timeLimit, workers, queryHandler, null);
	}

	//@ParameterNames(names={"timeLimit", "workers", "queryHandler", "warmup"})
	public Stresstest(Integer timeLimit, ArrayList workers, LinkedHashMap queryHandler, LinkedHashMap warmup) throws FileNotFoundException {
		this.timeLimit=timeLimit.doubleValue();
		this.workerConfig = workers;
		this.qhConfig = queryHandler;
		this.warmupConfig = warmup;
	}

	//@ParameterNames(names={"workers", "queryHandler", "noOfQueryMixesPerHour"})
	public Stresstest(ArrayList workers, LinkedHashMap queryHandler, Integer noOfQueryMixes) throws FileNotFoundException {
		this(workers, queryHandler, null, noOfQueryMixes);
	}

	//@ParameterNames(names={"workers", "queryHandler", "warmup", "noOfQueryMixesPerHour"})
	public Stresstest(ArrayList workers, LinkedHashMap queryHandler, LinkedHashMap warmup, Integer noOfQueryMixes) throws FileNotFoundException {
		this.noOfQueryMixes=noOfQueryMixes.longValue();
		this.workerConfig = workers;
		this.qhConfig = queryHandler;
		this.warmupConfig = warmup;
	}

	private void setConfig(ArrayList<HashMap> workers, HashMap queryHandler, HashMap warmup){
		if(warmup!=null){
			this.warmupTimeMS = ((Integer) warmup.get("timelimit")).doubleValue();
			this.warmupQueries = warmup.get("queries").toString();
			this.warmupUpdates = warmup.get("updates").toString();
		}
		for(HashMap workerConfig : workers){
			//let TypedFactory create from className and configuration
			String className = workerConfig.remove("className").toString();
			//if shorthand classname is used, exchange to full classname
			Integer threads = (Integer)workerConfig.remove("threads");
			noOfWorkers+=threads;
			workerConfig.put("connection", con);
			workerConfig.put("taskID", taskID);
			if(timeLimit!=null)
				workerConfig.put("timeLimit", timeLimit.intValue());
			for(int i=0;i<threads;i++) {
				workerConfig.put("workerID", i);
				Worker worker = new WorkerFactory().create(className, workerConfig);
				this.workers.add(worker);
			}
		}
		//let TypedFactory create queryHandlerConfiguration from className and configuration and add Workers
		this.qhClassName = queryHandler.get("className").toString();
		this.qhConfig = (HashMap)queryHandler.getOrDefault("configuration", new HashMap<>());
		qhConfig.put("workers", this.workers);
		addMetaData();
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
		QueryHandler queryHandler = factory.create(qhClassName, qhConfig);
		queryHandler.generateQueries();

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
			executor.awaitTermination(5, TimeUnit.SECONDS);
			LOGGER.info("[TaskID: {{}}] Task completed.", taskID);
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
	
	private void warmup() {
		if(warmupTimeMS==null||warmupTimeMS==0l) {
			return;
		}
		LinkedList<Worker> warmupWorkers = initWarmupWorkers();
		if(warmupWorkers.size()==0) {
			return;
		}
		QueryHandler iqh = new InstancesQueryHandler(warmupWorkers);
		iqh.generateQueries();
		LOGGER.info("[TaskID: {{}}] will start {{}}ms warmup now.", taskID, warmupTimeMS);
		executeWarmup(warmupWorkers);
	}
	
	private LinkedList<Worker> initWarmupWorkers(){
		LinkedList<Worker> warmupWorkers = new LinkedList<Worker>();

		if(warmupQueries!=null || warmupQueries.isEmpty()) {
			//TID WID, TL, SERVICE, USER, PWD, TimeOUT, q/u.txt, NL, NL
			SPARQLWorker sparql = new SPARQLWorker("-1", con, warmupQueries,null,null, 60000, warmupTimeMS.intValue(), 0, 0, 0);
			warmupWorkers.add(sparql);
			LOGGER.debug("[TaskID: {{}}] Warmup uses one SPARQL worker.", taskID);
		}
		if(warmupUpdates!=null || warmupUpdates.isEmpty()) {
			UPDATEWorker update = new UPDATEWorker("-1", con, warmupUpdates, null, 60000, warmupTimeMS.intValue(), 0,0, 0);
			warmupWorkers.add(update);
			LOGGER.debug("[TaskID: {{}}] Warmup uses one UPDATE worker", taskID);
		}	
		return warmupWorkers;
	}
	
	private void executeWarmup(List<Worker> warmupWorkers) {
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
				TimeUnit.MILLISECONDS.sleep(100);
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
		warmupWorkers.clear();
		LOGGER.info("[TaskID: {{}}] Warmup finished.", taskID);
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
				if (worker instanceof UPDATEWorker) {
					queriesInMix = ((AbstractWorker) worker).getNoOfQueries();

					if (worker.getExecutedQueries() / (queriesInMix * 1.0) >= 1) {
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
				else if (worker instanceof AbstractWorker) {
					queriesInMix = ((AbstractWorker) worker).getNoOfQueries();
					LOGGER.debug("No of query Mixes: {} , queriesInMix {}", worker.getExecutedQueries(),noOfQueryMixes);
					//Check for each worker, if the
					if (worker.getExecutedQueries() / (queriesInMix * 1.0) >= noOfQueryMixes.doubleValue()) {
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
			}
			return endFlag;
		}
		LOGGER.error("Neither time limit nor NoOfQueryMixes is set. executing task now");
		return true;
	}


}
