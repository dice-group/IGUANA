package org.aksw.iguana.cc.worker;

import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;


/**
 * The Abstract Worker which will implement the runnable, the main loop, the
 * time to wait before a query and will send the results to the ResultProcessor
 * module <br/>
 * so the Implemented Workers only need to implement which query to test next
 * and how to test this query.
 *
 * @author f.conrads
 */
public abstract class AbstractWorker implements Worker {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorker.class);

    protected String taskID;

    /**
     * The unique ID of the worker, should be from 0 to n
     */
    protected Integer workerID;

    /**
     * The worker Type. f.e. SPARQL or UPDATE or SQL or whatever
     * Determined by the Shorthand of the class, if no Shorthand is provided the class name is used.
     * The workerType is only used in logging messages.
     */
    protected String workerType;
    protected ConnectionConfig connection;
    protected Map<String, Object> queries;

    protected Double timeLimit;
    protected Double timeOut = 180000D;
    protected Integer fixedLatency = 0;
    protected Integer gaussianLatency = 0;

    protected boolean endSignal = false;
    protected long executedQueries;
    protected Instant startTime;
    protected ConnectionConfig con;
    protected int queryHash;
    protected QueryHandler queryHandler;
    private Collection<QueryExecutionStats> results = new LinkedList<>();
    private Random latencyRandomizer;
    private Long endAtNOQM = null;

    public AbstractWorker(String taskID, Integer workerID, ConnectionConfig connection, Map<String, Object> queries, @Nullable Integer timeLimit, @Nullable Integer timeOut, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency) {
        this.taskID = taskID;
        this.workerID = workerID;
        this.con = connection;

        handleTimeParams(timeLimit, timeOut, fixedLatency, gaussianLatency);
        setWorkerType();

        this.queryHandler = new QueryHandler(queries, this.workerID);

        LOGGER.debug("Initialized new Worker[{{}} : {{}}] for taskID {{}}", this.workerType, workerID, taskID);
    }


    @Override
    public void waitTimeMs() {
        double wait = this.fixedLatency.doubleValue();
        double gaussian = this.latencyRandomizer.nextDouble();
        wait += (gaussian * 2) * this.gaussianLatency;
        LOGGER.debug("Worker[{} : {}]: Time to wait for next Query {}", this.workerType, this.workerID, wait);
        try {
            if (wait > 0)
                Thread.sleep((int) wait);
        } catch (InterruptedException e) {
            LOGGER.error("Worker[{{}} : {}]: Could not wait time before next query due to: {}", this.workerType, this.workerID, e);
        }
    }

    /**
     * This will start the worker. It will get the next query, wait as long as it
     * should wait before executing the next query, then it will test the query and
     * send it if not aborted yet to the ResultProcessor Module
     */
    public void startWorker() {
        // For Update and Logging purpose get startTime of Worker
        this.startTime = Instant.now();

        this.queryHash = this.queryHandler.hashCode();

        LOGGER.info("Starting Worker[{{}} : {{}}].", this.workerType, this.workerID);
        // Execute Queries as long as the Stresstest will need.
        while (!this.endSignal && !hasExecutedNoOfQueryMixes(this.endAtNOQM)) {
            // Get next query
            StringBuilder query = new StringBuilder();
            StringBuilder queryID = new StringBuilder();
            try {
                getNextQuery(query, queryID);
                // check if endsignal was triggered
                if (this.endSignal) {
                    break;
                }
            } catch (IOException e) {
                LOGGER.error(
                        "Worker[{{}} : {{}}] : Something went terrible wrong in getting the next query. Worker will be shut down.",
                        this.workerType, this.workerID);
                LOGGER.error("Error which occured:_", e);
                break;
            }
            // Simulate Network Delay (or whatever should be simulated)
            waitTimeMs();

            // benchmark query
            try {
                executeQuery(query.toString(), queryID.toString());
            } catch (Exception e) {
                LOGGER.error("Worker[{{}} : {{}}] : ERROR with query: {{}}", this.workerType, this.workerID, query);
            }
            //this.executedQueries++;
        }
        LOGGER.info("Stopping Worker[{{}} : {{}}].", this.workerType, this.workerID);
    }

    @Override
    public void getNextQuery(StringBuilder query, StringBuilder queryID) throws IOException {
        this.queryHandler.getNextQuery(query, queryID);
    }

    protected HttpContext getAuthContext(String endpoint) {
        HttpClientContext context = HttpClientContext.create();

        if (this.con.getPassword() != null && this.con.getUser() != null && !this.con.getPassword().isEmpty() && !this.con.getUser().isEmpty()) {
            CredentialsProvider provider = new BasicCredentialsProvider();

            provider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(this.con.getUser(), this.con.getPassword()));

            //create target host
            String targetHost = endpoint;
            try {
                URI uri = new URI(endpoint);
                targetHost = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            //set Auth cache
            AuthCache authCache = new BasicAuthCache();
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(HttpHost.create(targetHost), basicAuth);

            context.setCredentialsProvider(provider);
            context.setAuthCache(authCache);

        }
        return context;
    }

    public synchronized void addResults(QueryExecutionStats results) {
        if (!this.endSignal && !hasExecutedNoOfQueryMixes(this.endAtNOQM)) {
            this.results.add(results);
            this.executedQueries++;

            //
            if (getNoOfQueries() > 0 && getExecutedQueries() % getNoOfQueries() == 0) {
                LOGGER.info("Worker executed {} queryMixes", getExecutedQueries() * 1.0 / getNoOfQueries());
            }
        }
    }

    @Override
    public synchronized Collection<QueryExecutionStats> popQueryResults() {
        if (this.results.isEmpty()) {
            return null;
        }
        Collection<QueryExecutionStats> ret = this.results;
        this.results = new LinkedList<>();
        return ret;
    }

    @Override
    public long getExecutedQueries() {
        return this.executedQueries;
    }

    @Override
    public void stopSending() {
        this.endSignal = true;
        LOGGER.debug("Worker[{{}} : {{}}] got stop signal.", this.workerType, this.workerID);
    }

    @Override
    public boolean isTerminated() {
        return this.endSignal;
    }


    @Override
    public void run() {
        startWorker();
    }

    @Override
    public long getNoOfQueries() {
        return this.queryHandler.getQueryCount();
    }

    @Override
    public boolean hasExecutedNoOfQueryMixes(Long noOfQueryMixes) {
        if (noOfQueryMixes == null) {
            return false;
        }
        return getExecutedQueries() / (getNoOfQueries() * 1.0) >= noOfQueryMixes;
    }

    @Override
    public void endAtNoOfQueryMixes(Long noOfQueryMixes) {
        this.endAtNOQM = noOfQueryMixes;
    }

    @Override
    public QueryHandler getQueryHandler() {
        return this.queryHandler;
    }

    private void handleTimeParams(Integer timeLimit, Integer timeOut, Integer fixedLatency, Integer gaussianLatency) {
        if (timeLimit != null) {
            this.timeLimit = timeLimit.doubleValue();
        }
        if (timeOut != null) {
            this.timeOut = timeOut.doubleValue();
        }
        if (fixedLatency != null) {
            this.fixedLatency = fixedLatency;
        }
        if (gaussianLatency != null) {
            this.gaussianLatency = gaussianLatency;
        }
        this.latencyRandomizer = new Random(this.workerID);
    }

    private void setWorkerType() {
        if (this.getClass().getAnnotation(Shorthand.class) != null) {
            this.workerType = this.getClass().getAnnotation(Shorthand.class).value();
        } else {
            this.workerType = this.getClass().getName();
        }
    }

    @Override
    public WorkerMetadata getMetadata() {
        return new WorkerMetadata(
                this.workerID,
                this.workerType,
                this.timeOut,
                this.queryHandler.getQueryCount(),
                this.queryHandler.hashCode(),
                this.queryHandler.getAllQueryIds()
        );
    }
}
