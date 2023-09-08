package org.aksw.iguana.cc.tasks.impl;

import org.aksw.iguana.cc.metrics.*;
import org.aksw.iguana.cc.storage.Storage;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.commons.rdf.IGUANA_BASE;
import org.aksw.iguana.commons.rdf.IONT;
import org.aksw.iguana.commons.rdf.IPROP;
import org.aksw.iguana.commons.rdf.IRES;
import org.aksw.iguana.commons.time.TimeUtils;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.*;

public class StresstestResultProcessor {

    private final List<Metric> metrics;

    private final List<HttpWorker> workers;
    private final List<String> queryIDs;
    private final List<Storage> storages;

    /**
     * This array contains each query execution, grouped by each worker and each query.
     */
    private List<HttpWorker.ExecutionStats>[][] workerQueryExecutions;

    /**
     * This map contains each query execution, grouped by each query of the task.
     */
    private Map<String, List<HttpWorker.ExecutionStats>> taskQueryExecutions;


    private final IRES.Factory iresFactory;


    public StresstestResultProcessor(long suiteID,
                                     long taskID,
                                     List<HttpWorker> worker,
                                     List<String> queryIDs,
                                     List<Storage> storages) {
        this.workers = worker;
        this.queryIDs = queryIDs;
        this.storages = storages;

        this.metrics = MetricManager.getMetrics(); // TODO: change this

        this.workerQueryExecutions = new List[workers.size()][];
        for (int i = 0; i < workers.size(); i++) {
            this.workerQueryExecutions[i] = new List[workers.get(i).config().queries().getQueryCount()];
            for (int j = 0; j < workers.get(i).config().queries().getQueryCount(); j++) {
                this.workerQueryExecutions[i][j] = new ArrayList<>();
            }
        }

        this.taskQueryExecutions = new HashMap<>();
        for (String queryID : queryIDs) {
            this.taskQueryExecutions.put(queryID, new ArrayList<>());
        }

        this.iresFactory = new IRES.Factory(suiteID, taskID);
    }

    /**
     * This method stores the given query executions statistics from a worker to their appropriate data location.
     *
     * @param data the query execution statistics that should be stored
     */
    public void process(Collection<HttpWorker.Result> data) {
        for (HttpWorker.Result result : data) {
            for (var stat : result.executionStats()) {
                workerQueryExecutions[(int) result.workerID()][stat.queryID()].add(stat);
                String queryID = workers.get((int) result.workerID()).config().queries().getQueryId(stat.queryID());
                taskQueryExecutions.get(queryID).add(stat);
            }
        }
    }

    /**
     * This method calculates the metrics and creates the RDF model of the result, which will be sent to the storages.
     * It uses the given data that was passed with the 'processQueryExecutions' method.
     *
     * @param start the start date of the task
     * @param end the end date of the task
     */
    public void calculateAndSaveMetrics(Calendar start, Calendar end, Model triplestats) {
        Model m = ModelFactory.createDefaultModel();
        Resource suiteRes = iresFactory.getSuiteResource();
        Resource taskRes = iresFactory.getTaskResource();

        m.add(suiteRes, RDF.type, IONT.suite);
        m.add(suiteRes, IPROP.task, taskRes);
        m.add(taskRes, RDF.type, IONT.task);
        m.add(taskRes, RDF.type, IONT.stresstest);
        m.add(taskRes, IPROP.noOfWorkers, ResourceFactory.createTypedLiteral(workers.size()));

        for (HttpWorker worker : workers) {
            HttpWorker.Config config = worker.config();

            Resource workerRes = iresFactory.getWorkerResource(worker);
            Resource connectionRes = IRES.getResource(config.connection().name());
            Resource datasetRes = IRES.getResource(config.connection().dataset().name()); // TODO: check if each connection only has one dataset

            m.add(taskRes, IPROP.workerResult, workerRes);
            m.add(workerRes, RDF.type, IONT.worker);
            m.add(workerRes, IPROP.workerID, ResourceFactory.createTypedLiteral(worker.getWorkerID()));
            m.add(workerRes, IPROP.workerType, ResourceFactory.createTypedLiteral(worker.getClass().getSimpleName()));
            m.add(workerRes, IPROP.noOfQueries, ResourceFactory.createTypedLiteral(config.queries().getQueryCount()));
            m.add(workerRes, IPROP.timeOut, TimeUtils.createTypedDurationLiteral(config.timeout()));
            if (config.completionTarget() instanceof HttpWorker.QueryMixes)
                m.add(taskRes, IPROP.noOfQueryMixes, ResourceFactory.createTypedLiteral(((HttpWorker.QueryMixes) config.completionTarget()).number()));
            if (config.completionTarget() instanceof HttpWorker.TimeLimit)
                m.add(taskRes, IPROP.timeLimit, TimeUtils.createTypedDurationLiteral(((HttpWorker.TimeLimit) config.completionTarget()).duration()));
            m.add(workerRes, IPROP.connection, connectionRes);

            m.add(connectionRes, RDF.type, IONT.connection);
            m.add(connectionRes, RDFS.label, ResourceFactory.createTypedLiteral(config.connection().name()));
            if (config.connection().version() != null) {
                m.add(connectionRes, IPROP.version, ResourceFactory.createTypedLiteral(config.connection().version()));
            }
            m.add(connectionRes, IPROP.dataset, datasetRes);

            m.add(datasetRes, RDFS.label, ResourceFactory.createTypedLiteral(config.connection().dataset().name())); // TODO: is this always correct? only one dataset per connection?
            m.add(datasetRes, RDF.type, IONT.dataset);
        }

        // TODO: language processor thingy

        // Connect task and workers to the Query nodes, that store the triple stats.
        for (var worker : workers) {
            var config = worker.config();
            var workerQueryIDs = config.queries().getAllQueryIds();
            for (int i = 0; i < config.queries().getQueryCount(); i++) {
                Resource workerQueryRes = iresFactory.getWorkerQueryResource(worker, i);
                Resource queryRes = IRES.getResource(workerQueryIDs[i]);
                m.add(workerQueryRes, IPROP.queryID, queryRes); // TODO: check this, (seems to be right)
            }

            var taskQueryIDs = this.queryIDs.toArray(String[]::new); // make elements accessible by index
            for (int i = 0; i < taskQueryIDs.length; i++) {
                Resource taskQueryRes = iresFactory.getTaskQueryResource(taskQueryIDs[i]);
                Resource queryRes = IRES.getResource(taskQueryIDs[i]);
                m.add(taskQueryRes, IPROP.queryID, queryRes); // TODO: check this as well
            }
        }

        for (Metric metric : metrics) {
            m.add(this.createMetricModel(metric));
        }

        // Task to queries
        for (String queryID : queryIDs) {
            m.add(taskRes, IPROP.query, iresFactory.getTaskQueryResource(queryID));
        }

        // Worker to queries
        for (var worker : workers) {
            for (int i = 0; i < worker.config().queries().getAllQueryIds().length; i++) {
                Resource workerRes = iresFactory.getWorkerResource(worker);
                m.add(workerRes, IPROP.query, iresFactory.getWorkerQueryResource(worker, i));
            }
        }

        m.add(taskRes, IPROP.startDate, ResourceFactory.createTypedLiteral(start));
        m.add(taskRes, IPROP.endDate, ResourceFactory.createTypedLiteral(end));

        m.setNsPrefixes(IGUANA_BASE.PREFIX_MAP);

        for (var storage : storages) {
            storage.storeResult(m);
        }
    }

    /**
     * For a given metric this method calculates the metric with the stored data and creates the appropriate
     * RDF related to that metric.
     *
     * @param metric the metric that should be calculated
     * @return the result model of the metric
     */
    private Model createMetricModel(Metric metric) {
        Model m = ModelFactory.createDefaultModel();
        Property metricProp = IPROP.createMetricProperty(metric);
        Resource metricRes = IRES.getMetricResource(metric);
        Resource taskRes = iresFactory.getTaskResource();

        if (metric instanceof ModelWritingMetric) {
            m.add(((ModelWritingMetric) metric).createMetricModel(this.workers,
                    this.workerQueryExecutions,
                    this.iresFactory));
            m.add(((ModelWritingMetric) metric).createMetricModel(this.workers,
                    this.taskQueryExecutions,
                    this.iresFactory));
        }

        if (metric instanceof TaskMetric) {
            Number metricValue = ((TaskMetric) metric).calculateTaskMetric(this.workers, workerQueryExecutions);
            if (metricValue != null) {
                Literal lit = ResourceFactory.createTypedLiteral(metricValue);
                m.add(taskRes, metricProp, lit);
            }
            m.add(taskRes, IPROP.metric, metricRes);
        }

        if (metric instanceof WorkerMetric) {
            for (var worker : workers) {
                Resource workerRes = iresFactory.getWorkerResource(worker);
                Number metricValue = ((WorkerMetric) metric).calculateWorkerMetric(
                        worker.config(),
                        workerQueryExecutions[(int) worker.getWorkerID()]);
                if (metricValue != null) {
                    Literal lit = ResourceFactory.createTypedLiteral(metricValue);
                    m.add(workerRes, metricProp, lit);
                }
                m.add(workerRes, IPROP.metric, metricRes);
            }
        }

        if (metric instanceof QueryMetric) {
            // queries grouped by worker
            for (var worker : workers) {
                for (int i = 0; i < worker.config().queries().getQueryCount(); i++) {
                    Number metricValue = ((QueryMetric) metric).calculateQueryMetric(workerQueryExecutions[(int) worker.getWorkerID()][i]);
                    if (metricValue != null) {
                        Literal lit = ResourceFactory.createTypedLiteral(metricValue);
                        Resource queryRes = iresFactory.getWorkerQueryResource(worker, i);
                        m.add(queryRes, metricProp, lit);
                    }
                }
            }

            // queries grouped by task
            for (String queryID : queryIDs) {
                Number metricValue = ((QueryMetric) metric).calculateQueryMetric(taskQueryExecutions.get(queryID));
                if (metricValue != null) {
                    Literal lit = ResourceFactory.createTypedLiteral(metricValue);
                    Resource queryRes = iresFactory.getTaskQueryResource(queryID);
                    m.add(queryRes, metricProp, lit);
                }
            }
        }

        m.add(metricRes, RDFS.label, metric.getName());
        m.add(metricRes, RDFS.label, metric.getAbbreviation());
        m.add(metricRes, RDFS.comment, metric.getDescription());
        m.add(metricRes, RDF.type, IONT.getMetricClass(metric));
        m.add(metricRes, RDF.type, IONT.metric);

        return m;
    }
}