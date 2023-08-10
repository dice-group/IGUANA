package org.aksw.iguana.cc.metrics.impl;

import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.metrics.ModelWritingMetric;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.commons.rdf.IPROP;
import org.aksw.iguana.commons.rdf.IRES;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.List;

public class EachExecutionStatistic extends Metric implements ModelWritingMetric {

    public EachExecutionStatistic() {
        super("Each Query Execution Statistic", "EachQuery", "This metric saves the statistics of each query execution.");
    }

    @Override
    @Nonnull
    public Model createMetricModel(List<HttpWorker> workers, List<HttpWorker.ExecutionStats>[][] data, IRES.Factory iresFactory) {
        Model m = ModelFactory.createDefaultModel();
        for (var worker : workers) {
            for (int i = 0; i < worker.config().queries().getQueryCount(); i++) {
                Resource workerQueryResource = iresFactory.getWorkerQueryResource(worker, i);
                Resource queryRes = IRES.getResource(worker.config().queries().getQueryId(i));
                BigInteger run = BigInteger.ONE;
                for (HttpWorker.ExecutionStats exec : data[(int) worker.getWorkerID()][i]) {
                    Resource runRes = iresFactory.getWorkerQueryRunResource(worker, i, run);
                    m.add(workerQueryResource, IPROP.queryExecution, runRes);
                    m.add(runRes, IPROP.time, ResourceFactory.createTypedLiteral(exec.duration()));
                    m.add(runRes, IPROP.success, ResourceFactory.createTypedLiteral(exec.successful()));
                    m.add(runRes, IPROP.run, ResourceFactory.createTypedLiteral(run));
                    m.add(runRes, IPROP.code, ResourceFactory.createTypedLiteral(exec.endState().value));
                    // TODO: maybe add http status code
                    if (exec.contentLength().isPresent())
                        m.add(runRes, IPROP.resultSize, ResourceFactory.createTypedLiteral(exec.contentLength().getAsLong()));
                    m.add(runRes, IPROP.queryID, queryRes);
                    run = run.add(BigInteger.ONE);
                }
            }
        }
        return m;
    }
}
