/**
 * 
 */
package org.aksw.iguana.tp.tasks.impl.stresstest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.tp.query.QueryHandler;
import org.aksw.iguana.tp.query.QueryHandlerFactory;
import org.aksw.iguana.tp.query.impl.InstancesQueryHandler;
import org.aksw.iguana.tp.tasks.AbstractTask;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.Worker;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.WorkerFactory;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.SPARQLWorker;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.UPDATEWorker;
import org.aksw.iguana.tp.utils.ConfigUtils;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the Stresstest. <br/>
 * Will initialize the {@link SPARQLWorker}s and {@link UPDATEWorker}s and
 * starts them as Threads. As soon as either the time limit was reached or the
 * query mixes / worker were executed it will send and endsignal to the
 * {@link Worker}s which then will end as soon as their last command was
 * executed (if and end signal occurs while the {@link Worker}s try to execute a
 * Query, the time the Query took will not be accounted in the results.
 * 
 * @author f.conrads
 *
 */
public class Stresstest extends AbstractTask {
 
	
	private static final Logger LOGGER = LoggerFactory
			.getLogger(Stresstest.class);
	
	private Long timeLimit;
	private Long noOfQueryMixes;
	private List<Worker> workers = new LinkedList<Worker>();
	private long startTime;
	private String qhClassName;
	private String[] qhConstructorArgs;
	private Long noOfWorkers=0l;

	private Long warmupTimeMS;
	private String warmupQueries;
	private String warmupUpdates;

	private String service;
	
	

	/**
	 * @param ids
	 * @param taskID
	 * @param service
	 * @param updateService
	 * @param taskConfig
	 */
	public Stresstest(String[] ids, String taskID, String service, String updateService, Configuration taskConfig) {
		
		super(ids, taskID, service, updateService);
		this.timeLimit = ConfigUtils.getObjectWithSuffix(taskConfig, "timeLimit");
		this.noOfQueryMixes = ConfigUtils.getObjectWithSuffix(taskConfig, "noOfQueryMixes");
		
		String[] tmp = ConfigUtils.getStringArrayWithSuffix(taskConfig, "queryHandler");
		this.qhClassName = tmp[0];
		this.qhConstructorArgs = Arrays.copyOfRange(tmp, 1, tmp.length);

		this.warmupTimeMS = ConfigUtils.getObjectWithSuffix(taskConfig, "warmupTimeMS");
		this.warmupQueries = ConfigUtils.getObjectWithSuffix(taskConfig, "warmupQueries");
		this.warmupUpdates = ConfigUtils.getObjectWithSuffix(taskConfig, "warmupUpdates");
		
		WorkerFactory factory = new WorkerFactory();
		Integer workerID = 0;
		// create Workers
		String[] workerConfigs = ConfigUtils.getStringArrayWithSuffix(taskConfig, "workers");
		
		for (String configKey : workerConfigs) {
			String[] workerConfig = taskConfig.getStringArray(configKey);
			int workers = Integer.parseInt(workerConfig[0]);
			noOfWorkers+=workers;
			for (int j = 0; j < workers; j++) {
				// set taskID, workerID, workerConfig
				String[] config = new String[2 + workerConfig.length];
				config[0] = taskID;
				config[1] = workerID.toString();
				workerID++;

				// sets null if timelimit is not defined otherwise the string repr. of the
				// timelimit
				config[2] = timeLimit == null ? null : timeLimit.toString();
				if(workerConfig[1].equals(UPDATEWorker.class.getCanonicalName())) {
					config[3] = updateService;
				}
				else {
					config[3] = service;
				}
				for (int i = 2; i < workerConfig.length; i++) {
					if(workerConfig[i]==null) {
						config[i+2] = null;
					}
					else {
						config[i + 2] = workerConfig[i];
					}
				}
				Worker worker = factory.create(workerConfig[1], new String[] {});
				worker.init(config);
				this.workers.add(worker);
			}
		}
		addMetaData();
	}
	

	/**
	 * Add extra Meta Data
	 */
	@Override
	public void addMetaData() {
		super.addMetaData();
		// TODO Future: add queries and update meta data
		Properties extraMeta = new Properties();
		if(timeLimit!=null)
			extraMeta.put("timeLimit", timeLimit);
		if(noOfQueryMixes!=null)
			extraMeta.put("noOfQueryMixes", noOfQueryMixes);
		extraMeta.put("noOfWorkers", noOfWorkers);
		this.metaData.put(COMMON.EXTRA_META_KEY, extraMeta);
	}

	@Override
	public void init(String host, String queueName) throws IOException, TimeoutException {
		super.init(host, queueName);

		// create from construct args and class
		QueryHandlerFactory factory = new QueryHandlerFactory();
		// add Worker
		QueryHandler queryHandler = factory.createWorkerBasedQueryHandler(qhClassName, qhConstructorArgs, workers);
		queryHandler.generateQueries();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aksw.iguana.tp.tasks.Task#start()
	 */
	@Override
	public void execute() {
		warmup();
		
		LOGGER.info("Task with ID {{}} will be executed now", this.taskID);
		// Execute each Worker in ThreadPool
		ExecutorService executor = Executors.newFixedThreadPool(noOfWorkers.intValue());
		this.startTime = Calendar.getInstance().getTimeInMillis();
		for (Worker worker : workers) {
			executor.submit(worker);
		}
		LOGGER.info("[TaskID: {{}}]All {{}} workers have been started", taskID, noOfWorkers);
		// wait timeLimit or noOfQueries
		while (!isFinished()) {
			// check if worker has results yet
			for (Worker worker : workers) {
				// if so send all results buffered
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
		}
		// tell all workers to stop sending properties, thus the await termination will
		// be safe with the results
		for (Worker worker : workers) {
			worker.stopSending();
		}
		// Wait 5seconds so the workers can stop themselves, otherwise they will be
		// stopped
		try {
			LOGGER.info("[TaskID: {{}}] Will shutdown and await termination in 5s.", taskID);
			executor.shutdown();
			executor.awaitTermination(5, TimeUnit.SECONDS);
			LOGGER.info("[TaskID: {{}}] Task completed.");
		} catch (InterruptedException e) {
			LOGGER.error("[TaskID: {{}}] Could not shutdown Threads/Workers due to ...", taskID);
			LOGGER.error("... Exception: ", e);
		}
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
		if(warmupQueries!=null) {
			SPARQLWorker sparql = new SPARQLWorker(new String[] {"-1", "1",  "0L", service, null,  warmupQueries, null, null});
			warmupWorkers.add(sparql);
			LOGGER.debug("[TaskID: {{}}] Warmup uses one SPARQL worker.", taskID);
		}
		if(warmupUpdates!=null) {
			UPDATEWorker update = new UPDATEWorker(new String[] {"-1", "2", "0L", service, null,  warmupUpdates, null,null, null, null});
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
		long start = Calendar.getInstance().getTimeInMillis();
		while((Calendar.getInstance().getTimeInMillis()-start) <= warmupTimeMS) {
			try {
				TimeUnit.MILLISECONDS.sleep(100);
			}catch(Exception e) {
				LOGGER.error("Could not warmup ");
			}
		}
		exec.shutdown();
		try {
			exec.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.warn("[TaskID: {{}}] Warmup. Could not await Termination of Workers.", taskID);
		}
		LOGGER.info("[TaskID: {{}}] Warmup finished.", taskID);
	}

	/**
	 * Checks if restriction (e.g. timelimit or noOfQueryMixes for each Worker)
	 * occurs
	 * 
	 * @return true if restriction occurs, false otherwise
	 */
	protected boolean isFinished() {
		if (timeLimit == null) {
			// use noOfQueries of SPARQLWorkers (as soon as a worker hit the noOfQueries, it
			// will stop sending results
			// UpdateWorker are allowed to execute all their updates
			boolean endFlag=true;
			for (Worker worker : workers) {
				long queriesInMix = 0;
				if (worker instanceof SPARQLWorker) {
					queriesInMix = ((AbstractWorker) worker).getNoOfQueries();
					if (worker.getExecutedQueries() / queriesInMix * 1.0 >= noOfQueryMixes) {

						worker.stopSending();
					}
					else {
						endFlag = false;
					}
				} else if (worker instanceof UPDATEWorker) {
					queriesInMix = ((AbstractWorker) worker).getNoOfQueries();

					if (worker.getExecutedQueries() / queriesInMix * 1.0 >= 1) {
						worker.stopSending();
					}
					else {
						endFlag = false;
					}
				}
			}
			return endFlag;
		}
		return timeLimit - (Calendar.getInstance().getTimeInMillis() - this.startTime) <= 0;
	}

	@Override
	public boolean isValid(Properties configuration) {
		// TODO For Future: validate Config first
		return true;
	}

}
