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

import org.aksw.iguana.tp.query.QueryHandler;
import org.aksw.iguana.tp.query.QueryHandlerFactory;
import org.aksw.iguana.tp.tasks.AbstractTask;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.Worker;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.WorkerFactory;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.SPARQLWorker;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.UPDATEWorker;
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

	/**
	 * 
	 * The objects of the workerConfiguration has to be in the following order:<br/>
	 * <ol>
	 * <li>number of workers to create with this config</li>
	 * <li>class name of the worker, e.g.
	 * org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.SPARQLWorker</li>
	 * <li>all other constructor arguments the worker needs</li>
	 * </ol>
	 * 
	 * @param taskID
	 *            the current TaskID
	 * @param timeLimit
	 *            can be safely null if noOfQueryMixes is set
	 * @param noOfQueryMixes
	 *            can be safely null if timeLimit is set
	 * @param workerConfigurations
	 *            configurations of the workers to create
	 * @param queryHandler
	 *            the class name and constructor args of a Worker Based Query
	 *            Handler
	 */
	public Stresstest(String taskID, Long timeLimit, Long noOfQueryMixes, Object[][] workerConfigurations,
			String[] queryHandler) {
		super(taskID);
		this.timeLimit = timeLimit;
		this.noOfQueryMixes = noOfQueryMixes;

		this.qhClassName = queryHandler[0];
		this.qhConstructorArgs = Arrays.copyOfRange(queryHandler, 1, queryHandler.length);

		WorkerFactory factory = new WorkerFactory();
		Integer workerID = 0;
		// create Workers
		for (Object[] workerConfig : workerConfigurations) {
			int workers = Integer.parseInt(workerConfig[0].toString());
			for (int j = 0; j < workers; j++) {
				// set taskID, workerID, workerConfig
				String[] config = new String[3 + workerConfig.length];
				config[0] = taskID;
				config[1] = workerID.toString();
				workerID++;

				// sets null if timelimit is not defined otherwise the string repr. of the
				// timelimit
				config[2] = timeLimit == null ? null : timeLimit.toString();
				for (int i = 3; i < workerConfig.length; i++) {

					config[i + 1] = workerConfig[i].toString();
				}
				;
				this.workers.add(factory.create(config[1], config));
			}
		}
		addMetaData();
	}

	/**
	 * Add extra Meta Data
	 */
	private void addMetaData() {
		// TODO Future: add queries and update meta data
		this.metaData.put("timeLimit", timeLimit);
		this.metaData.put("noOfQueryMixes", noOfQueryMixes);
		this.metaData.put("noOfWorkers", workers.size());
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
		LOGGER.info("Task with ID {{}} will be executed now", this.taskID);
		// Execute each Worker in ThreadPool
		ExecutorService executor = Executors.newFixedThreadPool(workers.size());
		this.startTime = Calendar.getInstance().getTimeInMillis();
		for (Worker worker : workers) {
			executor.submit(worker);
		}
		LOGGER.info("[TaskID: {{}}]All {{}} workers have been started", taskID, workers.size());
		// wait timeLimit or noOfQueries
		while (isFinished()) {
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
			for (Worker worker : workers) {
				long queriesInMix = 0;
				if (worker instanceof SPARQLWorker) {
					queriesInMix = ((AbstractWorker) worker).getNoOfQueries();
					if (worker.getExecutedQueries() / queriesInMix * 1.0 >= noOfQueryMixes) {

						worker.stopSending();
					}
				} else if (worker instanceof UPDATEWorker) {
					queriesInMix = ((AbstractWorker) worker).getNoOfQueries();

					if (worker.getExecutedQueries() / queriesInMix * 1.0 >= 1) {
						worker.stopSending();
					}
				}
			}
		}
		return timeLimit - (Calendar.getInstance().getTimeInMillis() - this.startTime) <= 0;
	}

	@Override
	public boolean isValid(Properties configuration) {
		// TODO For Future: validate Config first
		return true;
	}

}
