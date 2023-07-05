package org.aksw.iguana.cc.tasks.stresstest;

import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.worker.WorkerMetadata;
import org.aksw.iguana.cc.tasks.stresstest.metrics.*;
import org.aksw.iguana.cc.tasks.stresstest.storage.StorageManager;
import org.aksw.iguana.commons.rdf.IONT;
import org.aksw.iguana.commons.rdf.IPROP;
import org.aksw.iguana.commons.rdf.IRES;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.*;

public class StresstestResultProcessor {

    private final StresstestMetadata metadata;
    private final List<Metric> metrics;

    /**
     * This array contains each query execution, grouped by each worker and each query.
     */
    private List<QueryExecutionStats>[][] workerQueryExecutions;

    /**
     * This map contains each query execution, grouped by each query of the task.
     */
    private Map<String, List<QueryExecutionStats>> taskQueryExecutions;

    private final Resource taskRes;

    public StresstestResultProcessor(StresstestMetadata metadata) {
        this.metadata = metadata;
        this.taskRes = IRES.getResource(metadata.taskID());
        this.metrics = MetricManager.getMetrics();

        WorkerMetadata[] workers = metadata.workers();
        this.workerQueryExecutions = new List[workers.length][];
        for (int i = 0; i < workers.length; i++) {
            this.workerQueryExecutions[i] = new List[workers[i].numberOfQueries()];
            for (int j = 0; j < workers[i].numberOfQueries(); j++) {
                this.workerQueryExecutions[i][j] = new LinkedList<>();
            }
        }
        taskQueryExecutions = new HashMap<>();
    }

    /**
     * This method stores the given query executions statistics from a worker to their appropriate data location.
     *
     * @param worker the worker that has executed the queries
     * @param data a collection of the query execution statistics
     */
    public void processQueryExecutions(WorkerMetadata worker, Collection<QueryExecutionStats> data) {
        for(QueryExecutionStats stat : data) {
            // The queryIDs returned by the queryHandler are Strings, in the form of '<queryhandler_name>:<id>'.
            int queryID = Integer.parseInt(stat.queryID().substring(stat.queryID().indexOf(":") + 1));
            workerQueryExecutions[worker.workerID()][queryID].add(stat);

            if (taskQueryExecutions.containsKey(stat.queryID())) {
                taskQueryExecutions.get(stat.queryID()).add(stat);
            } else {
                taskQueryExecutions.put(stat.queryID(), new LinkedList<>());
                taskQueryExecutions.get(stat.queryID()).add(stat);
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
    public void calculateAndSaveMetrics(Calendar start, Calendar end) {
        Model m = ModelFactory.createDefaultModel();
        Resource suiteRes = IRES.getResource(metadata.suiteID());
        Resource experimentRes = IRES.getResource(metadata.expID());
        Resource datasetRes = IRES.getResource(metadata.datasetID());
        Resource connectionRes = IRES.getResource(metadata.conID());

        m.add(suiteRes, IPROP.experiment, experimentRes);
        m.add(suiteRes, RDF.type, IONT.suite);
        m.add(experimentRes, IPROP.dataset, datasetRes);
        m.add(experimentRes, RDF.type, IONT.experiment);
        m.add(experimentRes, IPROP.task, taskRes);
        m.add(datasetRes, RDFS.label, ResourceFactory.createTypedLiteral(metadata.datasetID()));
        m.add(datasetRes, RDF.type, IONT.dataset);
        m.add(taskRes, IPROP.connection, connectionRes);
        if (metadata.noOfQueryMixes().isPresent())
            m.add(taskRes, IPROP.noOfQueryMixes, ResourceFactory.createTypedLiteral(metadata.noOfQueryMixes().get()));
        m.add(taskRes, IPROP.noOfWorkers, ResourceFactory.createTypedLiteral(metadata.workers().length));
        if (metadata.timelimit().isPresent())
            m.add(taskRes, IPROP.timeLimit, ResourceFactory.createTypedLiteral(metadata.timelimit().get()));
        m.add(taskRes, RDF.type, IONT.task);

        m.add(taskRes, RDF.type, IONT.getClass(metadata.classname()));
        if (metadata.conVersion().isPresent())
            m.add(connectionRes, IPROP.version, ResourceFactory.createTypedLiteral(metadata.conVersion().get()));
        m.add(connectionRes, RDFS.label, ResourceFactory.createTypedLiteral(metadata.conID()));
        m.add(connectionRes, RDF.type, IONT.connection);

        for (WorkerMetadata worker : metadata.workers()) {
            Resource workerRes = IRES.getWorkerResource(metadata.taskID(), worker.workerID());
            m.add(taskRes, IPROP.workerResult, workerRes);
            m.add(workerRes, IPROP.workerID, ResourceFactory.createTypedLiteral(worker.workerID()));
            m.add(workerRes, IPROP.workerType, ResourceFactory.createTypedLiteral(worker.workerType()));
            m.add(workerRes, IPROP.noOfQueries, ResourceFactory.createTypedLiteral(worker.queryIDs().length));
            m.add(workerRes, IPROP.timeOut, ResourceFactory.createTypedLiteral(worker.timeout()));
            m.add(workerRes, RDF.type, IONT.worker);
        }

        if (metadata.tripleStats().isPresent()) {
            m.add(metadata.tripleStats().get());
            // Connect task and workers to the Query nodes, that store the triple stats.
            for (WorkerMetadata worker : metadata.workers()) {
                for (String queryID : worker.queryIDs()) {
                    Resource workerQueryRes = IRES.getWorkerQueryResource(metadata.taskID(), worker.workerID(), queryID);
                    Resource queryRes = IRES.getResource(worker.queryHash() + "/" + queryID);
                    m.add(workerQueryRes, IPROP.queryID, queryRes);
                }

                for (String queryID : metadata.queryIDs()) {
                    Resource taskQueryRes = IRES.getTaskQueryResource(metadata.taskID(), queryID);
                    Resource queryRes = IRES.getResource(worker.queryHash() + "/" + queryID);
                    m.add(taskQueryRes, IPROP.queryID, queryRes);
                }
            }
        }

        for (Metric metric : metrics) {
            m.add(this.createMetricModel(metric));
        }

        // Task to queries
        for (String queryID : metadata.queryIDs()) {
            m.add(taskRes, IPROP.query, IRES.getTaskQueryResource(metadata.taskID(), queryID));
        }

        // Worker to queries
        for (WorkerMetadata worker : metadata.workers()) {
            for (String queryID : worker.queryIDs()) {
                Resource workerRes = IRES.getWorkerResource(metadata.taskID(), worker.workerID());
                m.add(workerRes, IPROP.query, IRES.getWorkerQueryResource(metadata.taskID(), worker.workerID(), queryID));
            }
        }

        m.add(taskRes, IPROP.startDate, ResourceFactory.createTypedLiteral(start));
        m.add(taskRes, IPROP.endDate, ResourceFactory.createTypedLiteral(end));

        m.setNsPrefix(IPROP.PREFIX, IPROP.NS);
        m.setNsPrefix(IONT.PREFIX, IONT.NS);
        m.setNsPrefix(IRES.PREFIX, IRES.NS);
        m.setNsPrefix("lsqr", "http://lsq.aksw.org/res/");

        StorageManager.getInstance().storeResult(m);
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

        if (metric instanceof ModelWritingMetric) {
            m.add(((ModelWritingMetric) metric).createMetricModel(metadata, workerQueryExecutions));
            m.add(((ModelWritingMetric) metric).createMetricModel(metadata, taskQueryExecutions));
        }

        if (metric instanceof TaskMetric) {
            Number metricValue = ((TaskMetric) metric).calculateTaskMetric(metadata, workerQueryExecutions);
            if (metricValue != null) {
                Literal lit = ResourceFactory.createTypedLiteral(metricValue);
                m.add(taskRes, metricProp, lit);
            }
            m.add(taskRes, IPROP.metric, metricRes);
        }

        if (metric instanceof WorkerMetric) {
            for (WorkerMetadata worker : metadata.workers()) {
                Resource workerRes = IRES.getWorkerResource(metadata.taskID(), worker.workerID());
                Number metricValue = ((WorkerMetric) metric).calculateWorkerMetric(worker, workerQueryExecutions[worker.workerID()]);
                if (metricValue != null) {
                    Literal lit = ResourceFactory.createTypedLiteral(metricValue);
                    m.add(workerRes, metricProp, lit);
                }
                m.add(workerRes, IPROP.metric, metricRes);
            }
        }

        if (metric instanceof QueryMetric) {
            // queries grouped by worker
            for (WorkerMetadata worker : metadata.workers()) {
                for (int i = 0; i < worker.numberOfQueries(); i++) {
                    Number metricValue = ((QueryMetric) metric).calculateQueryMetric(workerQueryExecutions[worker.workerID()][i]);
                    if (metricValue != null) {
                        Literal lit = ResourceFactory.createTypedLiteral(metricValue);
                        Resource queryRes = IRES.getWorkerQueryResource(metadata.taskID(), worker.workerID(), worker.queryIDs()[i]);
                        m.add(queryRes, metricProp, lit);
                    }
                }
            }

            // queries grouped by task
            for (String queryID : taskQueryExecutions.keySet()) {
                Number metricValue = ((QueryMetric) metric).calculateQueryMetric(taskQueryExecutions.get(queryID));
                if (metricValue != null) {
                    Literal lit = ResourceFactory.createTypedLiteral(metricValue);
                    Resource queryRes = IRES.getTaskQueryResource(metadata.taskID(), queryID);
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
