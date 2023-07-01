package org.aksw.iguana.cc.tasks.stresstest;

import org.aksw.iguana.cc.config.CONSTANTS;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.model.StresstestMetadata;
import org.aksw.iguana.cc.model.WorkerMetadata;
import org.aksw.iguana.cc.tasks.AbstractTask;
import org.aksw.iguana.cc.worker.Worker;
import org.aksw.iguana.cc.worker.WorkerFactory;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(Stresstest.class);

    private final Map<String, Object> warmupConfig;
    private final List<Worker> warmupWorkers = new ArrayList<>();
    private final List<Map<String, Object>> workerConfig;
    protected List<Worker> workers = new LinkedList<>();
    private Double warmupTimeMS;
    private Double timeLimit;
    private Long noOfQueryMixes;
    private Instant startTime;


    public Stresstest(Integer timeLimit, List<Map<String, Object>> workers) {
        this(timeLimit, workers, null);
    }

    public Stresstest(Integer timeLimit, List<Map<String, Object>> workers, Map<String, Object> warmup) {
        this.timeLimit = timeLimit.doubleValue();
        this.workerConfig = workers;
        this.warmupConfig = warmup;
    }

    public Stresstest(List<Map<String, Object>> workers, Integer noOfQueryMixes) {
        this(workers, null, noOfQueryMixes);
    }

    public Stresstest(List<Map<String, Object>> workers, Map<String, Object> warmup, Integer noOfQueryMixes) {
        this.noOfQueryMixes = noOfQueryMixes.longValue();
        this.workerConfig = workers;
        this.warmupConfig = warmup;
    }

    private void initWorkers() {
        if (this.warmupConfig != null) {
            createWarmupWorkers();
        }
        createWorkers();
    }

    private void createWarmupWorkers() {
        this.warmupTimeMS = ((Integer) this.warmupConfig.get("timeLimit")).doubleValue();

        List<Map<String, Object>> warmupWorkerConfig = (List<Map<String, Object>>) this.warmupConfig.get("workers");
        createWorkers(warmupWorkerConfig, this.warmupWorkers, this.warmupTimeMS);
    }

    private void createWorkers() {
        createWorkers(this.workerConfig, this.workers, this.timeLimit);
    }

    private void createWorkers(List<Map<String, Object>> workers, List<Worker> workersToAddTo, Double timeLimit) {
        int workerID = 0;
        for (Map<String, Object> workerConfig : workers) {
            workerID += createWorker(workerConfig, workersToAddTo, timeLimit, workerID);
        }
    }

    private int createWorker(Map<String, Object> workerConfig, List<Worker> workersToAddTo, Double timeLimit, Integer baseID) {
        //let TypedFactory create from className and configuration
        String className = workerConfig.remove("className").toString();
        //if shorthand classname is used, exchange to full classname
        workerConfig.put("connection", this.con);
        workerConfig.put("taskID", this.taskID);

        if (timeLimit != null) {
            workerConfig.put("timeLimit", timeLimit.intValue());
        }
        Integer threads = (Integer) workerConfig.remove("threads");
        for (int i = 0; i < threads; i++) {
            workerConfig.put("workerID", baseID + i);
            Worker worker = new WorkerFactory().create(className, workerConfig);
            if (this.noOfQueryMixes != null) {
                worker.endAtNoOfQueryMixes(this.noOfQueryMixes);
            }
            workersToAddTo.add(worker);
        }
        return threads;
    }

    public void generateTripleStats() {
        StringWriter sw = new StringWriter();
        Model tripleStats = ModelFactory.createDefaultModel();
        for (Worker worker : this.workers) {
            tripleStats.add(worker.getQueryHandler().getTripleStats(this.taskID));
        }
        RDFDataMgr.write(sw, tripleStats, RDFFormat.NTRIPLES);
        this.metaData.put(COMMON.SIMPLE_TRIPLE_KEY, sw.toString());
        this.metaData.put(COMMON.QUERY_STATS, tripleStats);
    }

    /**
     * Add extra Meta Data
     */
    @Override
    public void addMetaData() {
        super.addMetaData();
        Properties extraMeta = new Properties();
        if (this.timeLimit != null)
            extraMeta.put(CONSTANTS.TIME_LIMIT, this.timeLimit);
        if (this.noOfQueryMixes != null)
            extraMeta.put(CONSTANTS.NO_OF_QUERY_MIXES, this.noOfQueryMixes);
        extraMeta.put("noOfWorkers", this.workers.size());
        this.metaData.put(COMMON.EXTRA_META_KEY, extraMeta);
    }


    @Override
    public void init(String[] ids, String dataset, ConnectionConfig connection, String taskName) {
        super.init(ids, dataset, connection, taskName);

        initWorkers();
        addMetaData();
        generateTripleStats();
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
        ExecutorService executor = Executors.newFixedThreadPool(this.workers.size());
        this.startTime = Instant.now();
        for (Worker worker : this.workers) {
            executor.execute(worker);
        }
        LOGGER.info("[TaskID: {{}}]All {{}} workers have been started", this.taskID, this.workers.size());
        // wait timeLimit or noOfQueries
        executor.shutdown();
        while (!isFinished()) {
            // check if worker has results yet
            for (Worker worker : this.workers) {
                // if so send all results buffered
                sendWorkerResult(worker);
            }
            loopSleep();
        }
        LOGGER.debug("Sending stop signal to workers");
        // tell all workers to stop sending properties, thus the await termination will
        // be safe with the results
        for (Worker worker : this.workers) {
            worker.stopSending();
        }
        // Wait 5seconds so the workers can stop themselves, otherwise they will be
        // stopped
        try {
            LOGGER.debug("Will shutdown now...");

            LOGGER.info("[TaskID: {{}}] Will shutdown and await termination in 5s.", this.taskID);
            boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);
            LOGGER.info("[TaskID: {{}}] Task completed. Thread finished status {}", this.taskID, finished);
        } catch (InterruptedException e) {
            LOGGER.error("[TaskID: {{}}] Could not shutdown Threads/Workers due to ...", this.taskID);
            LOGGER.error("... Exception: ", e);
            try {
                executor.shutdownNow();
            } catch (Exception e1) {
                LOGGER.error("Problems shutting down", e1);
            }
        }
    }

    private void loopSleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (Exception e) {
            //shouldn't be thrown except something else really went wrong
            LOGGER.error("Loop sleep did not work.", e);
        }
    }

    private void sendWorkerResult(Worker worker) {
        Collection<Properties> props = worker.popQueryResults();
        if (props == null) {
            return;
        }

        for (Properties results : props) {
            try {

                // send results via RabbitMQ
                LOGGER.debug("[TaskID: {{}}] Send results", this.taskID);
                this.sendResults(results);
                LOGGER.debug("[TaskID: {{}}] results could be send", this.taskID);
            } catch (IOException e) {
                LOGGER.error("[TaskID: {{}}] Could not send results due to exc.", this.taskID, e);
                LOGGER.error("[TaskID: {{}}] Results: {{}}", this.taskID, results);
            }
        }
    }


    @Override
    public void close() {
        super.close();
    }

    protected long warmup() {
        if (this.warmupTimeMS == null || this.warmupTimeMS == 0L) {
            return 0;
        }
        if (this.warmupWorkers.size() == 0) {
            return 0;
        }
        LOGGER.info("[TaskID: {{}}] will start {{}}ms warmup now using {} no of workers in total.", this.taskID, this.warmupTimeMS, this.warmupWorkers.size());
        return executeWarmup(this.warmupWorkers);
    }


    private long executeWarmup(List<Worker> warmupWorkers) {
        ExecutorService exec = Executors.newFixedThreadPool(2);
        for (Worker worker : warmupWorkers) {
            exec.submit(worker);
        }
        //wait as long as needed
        Instant start = Instant.now();
        exec.shutdown();
        while (durationInMilliseconds(start, Instant.now()) <= this.warmupTimeMS) {
            //clean up RAM
            for (Worker worker : warmupWorkers) {
                worker.popQueryResults();
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (Exception e) {
                LOGGER.error("Could not warmup ");
            }
        }
        for (Worker worker : warmupWorkers) {
            worker.stopSending();
        }
        try {
            exec.awaitTermination(5, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            LOGGER.warn("[TaskID: {{}}] Warmup. Could not await Termination of Workers.", this.taskID);
        }
        try {
            exec.shutdownNow();
        } catch (Exception e1) {
            LOGGER.error("Shutdown problems ", e1);
        }
        //clear up
        long queriesExec = 0;
        for (Worker w : warmupWorkers) {
            queriesExec += w.getExecutedQueries();
        }
        warmupWorkers.clear();
        LOGGER.info("[TaskID: {{}}] Warmup finished.", this.taskID);
        return queriesExec;
    }

    /**
     * Checks if restriction (e.g. timelimit or noOfQueryMixes for each Worker)
     * occurs
     *
     * @return true if restriction occurs, false otherwise
     */
    protected boolean isFinished() {
        if (this.timeLimit != null) {

            Instant current = Instant.now();
            double passed_time = this.timeLimit - durationInMilliseconds(this.startTime, current);
            return passed_time <= 0D;
        } else if (this.noOfQueryMixes != null) {

            // use noOfQueries of SPARQLWorkers (as soon as a worker hit the noOfQueries, it
            // will stop sending results
            // UpdateWorker are allowed to execute all their updates
            boolean endFlag = true;
            for (Worker worker : this.workers) {
                LOGGER.debug("No of query Mixes: {} , queriesInMix {}", this.noOfQueryMixes, worker.getExecutedQueries());
                //Check for each worker, if the
                if (worker.hasExecutedNoOfQueryMixes(this.noOfQueryMixes)) {
                    if (!worker.isTerminated()) {
                        //if the worker was not already terminated, send last results, as tehy will not be sended afterwards
                        sendWorkerResult(worker);
                    }
                    worker.stopSending();
                } else {
                    endFlag = false;
                }

            }
            return endFlag;
        }
        LOGGER.error("Neither time limit nor NoOfQueryMixes is set. executing task now");
        return true;
    }

    public long getExecutedQueries() {
        long ret = 0;
        for (Worker worker : this.workers) {
            ret += worker.getExecutedQueries();
        }
        return ret;
    }

    public StresstestMetadata getMetadata() {
        String classname;
        if (this.getClass().isAnnotationPresent(Shorthand.class)) {
            classname = this.getClass().getAnnotation(Shorthand.class).value();
        } else {
            classname = this.getClass().getCanonicalName();
        }

        Set<String> queryIDs = new HashSet<>();
        WorkerMetadata[] workerMetadata = new WorkerMetadata[this.workers.size()];
        for (int i = 0; i < this.workers.size(); i++) {
            workerMetadata[i] = this.workers.get(i).getMetadata();
            queryIDs.addAll(Arrays.asList(workerMetadata[i].queryIDs()));
        }

        StringWriter sw = new StringWriter();
        Model tripleStats = ModelFactory.createDefaultModel();
        for (Worker worker : this.workers) {
            tripleStats.add(worker.getQueryHandler().getTripleStats(this.taskID));
        }
        RDFDataMgr.write(sw, tripleStats, RDFFormat.NTRIPLES);

        return new StresstestMetadata(
                suiteID,
                expID,
                taskID,
                datasetID,
                conID,
                con.getVersion(),
                taskName,
                classname,
                this.timeLimit,
                this.noOfQueryMixes,
                workerMetadata,
                queryIDs,
                sw.toString(),
                tripleStats
        );
    }
}
