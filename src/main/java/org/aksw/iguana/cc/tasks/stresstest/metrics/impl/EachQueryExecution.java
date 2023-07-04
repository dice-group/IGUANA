package org.aksw.iguana.cc.tasks.stresstest.metrics.impl;

import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.model.StresstestMetadata;
import org.aksw.iguana.cc.model.WorkerMetadata;
import org.aksw.iguana.cc.tasks.stresstest.metrics.Metric;
import org.aksw.iguana.cc.tasks.stresstest.metrics.ModelWritingMetric;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.rdf.IPROP;
import org.aksw.iguana.commons.rdf.IRES;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.List;

@Shorthand("EachQuery")
public class EachQueryExecution extends Metric implements ModelWritingMetric {

    public EachQueryExecution() {
        super("Each Query Execution", "EachQuery", "This metric saves the statistics of each query execution.");
    }

    @Override
    @Nonnull
    public Model createMetricModel(StresstestMetadata task, List<QueryExecutionStats>[][] data) {
        Model m = ModelFactory.createDefaultModel();
        for (WorkerMetadata worker : task.workers()) {
            for (int i = 0; i < worker.numberOfQueries(); i++) {
                Resource queryRes = IRES.getWorkerQueryResource(task.taskID(), worker.workerID(), worker.queryIDs()[i]);
                Resource query = IRES.getResource(worker.queryHash() + "/" + worker.queryIDs()[i]);
                BigInteger run = BigInteger.ONE;
                for (QueryExecutionStats exec : data[worker.workerID()][i]) {
                    Resource runRes = IRES.getWorkerQueryRunResource(task.taskID(), worker.workerID(), worker.queryIDs()[i], run);
                    m.add(queryRes, IPROP.queryExecution, runRes);
                    m.add(runRes, IPROP.time, ResourceFactory.createTypedLiteral(exec.executionTime()));
                    m.add(runRes, IPROP.success, ResourceFactory.createTypedLiteral(exec.responseCode() == COMMON.QUERY_SUCCESS));
                    m.add(runRes, IPROP.run, ResourceFactory.createTypedLiteral(run));
                    m.add(runRes, IPROP.code, ResourceFactory.createTypedLiteral(exec.responseCode()));
                    m.add(runRes, IPROP.resultSize, ResourceFactory.createTypedLiteral(exec.resultSize()));
                    m.add(runRes, IPROP.queryID, query);
                    run = run.add(BigInteger.ONE);
                }
            }
        }
        return m;
    }
}
