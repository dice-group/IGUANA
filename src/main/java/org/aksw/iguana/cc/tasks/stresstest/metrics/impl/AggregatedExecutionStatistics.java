package org.aksw.iguana.cc.tasks.stresstest.metrics.impl;

import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.tasks.stresstest.StresstestMetadata;
import org.aksw.iguana.cc.worker.WorkerMetadata;
import org.aksw.iguana.cc.tasks.stresstest.metrics.Metric;
import org.aksw.iguana.cc.tasks.stresstest.metrics.ModelWritingMetric;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.rdf.IONT;
import org.aksw.iguana.commons.rdf.IPROP;
import org.aksw.iguana.commons.rdf.IRES;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.aksw.iguana.commons.time.TimeUtils.toXSDDurationInSeconds;

@Shorthand("AES")
public class AggregatedExecutionStatistics extends Metric implements ModelWritingMetric {

    public AggregatedExecutionStatistics() {
        super("Aggregated Execution Statistics", "AES", "Sums up the statistics of each query execution for each query a worker and task has. The result size only contains the value of the last execution.");
    }

    @Override
    @Nonnull
    public Model createMetricModel(StresstestMetadata task, List<QueryExecutionStats>[][] data) {
        Model m = ModelFactory.createDefaultModel();
        for (WorkerMetadata worker : task.workers()) {
            for (int i = 0; i < worker.numberOfQueries(); i++) {
                Resource queryRes = IRES.getWorkerQueryResource(task.taskID(), worker.workerID(), worker.queryIDs()[i]);
                m.add(createAggregatedModel(data[worker.workerID()][i], queryRes));
            }
        }
        return m;
    }

    @Override
    @Nonnull
    public Model createMetricModel(StresstestMetadata task, Map<String, List<QueryExecutionStats>> data) {
        Model m = ModelFactory.createDefaultModel();
        for (String queryID : data.keySet()) {
            Resource queryRes = IRES.getTaskQueryResource(task.taskID(), queryID);
            m.add(createAggregatedModel(data.get(queryID), queryRes));
        }
        return m;
    }

    private static Model createAggregatedModel(List<QueryExecutionStats> data, Resource queryRes) {
        Model m = ModelFactory.createDefaultModel();
        BigInteger succeeded = BigInteger.ZERO;
        BigInteger failed = BigInteger.ZERO;
        BigInteger resultSize = BigInteger.ZERO;
        BigInteger wrongCodes = BigInteger.ZERO;
        BigInteger timeOuts = BigInteger.ZERO;
        BigInteger unknownExceptions = BigInteger.ZERO;
        Duration totalTime = Duration.ZERO;

        for (QueryExecutionStats exec : data) {
            // TODO: make response code integer
            switch ((int) exec.responseCode()) {
                case (int) COMMON.QUERY_SUCCESS -> succeeded = succeeded.add(BigInteger.ONE);
                case (int) COMMON.QUERY_SOCKET_TIMEOUT -> {
                    timeOuts = timeOuts.add(BigInteger.ONE);
                    failed = failed.add(BigInteger.ONE);
                }
                case (int) COMMON.QUERY_HTTP_FAILURE -> {
                    wrongCodes = wrongCodes.add(BigInteger.ONE);
                    failed = failed.add(BigInteger.ONE);
                }
                case (int) COMMON.QUERY_UNKNOWN_EXCEPTION -> {
                    unknownExceptions = unknownExceptions.add(BigInteger.ONE);
                    failed = failed.add(BigInteger.ONE);
                }
            }

            totalTime = totalTime.plusNanos((long) (exec.executionTime() * 1000000));
            resultSize = BigInteger.valueOf(exec.resultSize());
        }

        m.add(queryRes, IPROP.succeeded, ResourceFactory.createTypedLiteral(succeeded));
        m.add(queryRes, IPROP.failed, ResourceFactory.createTypedLiteral(failed));
        m.add(queryRes, IPROP.resultSize, ResourceFactory.createTypedLiteral(resultSize));
        m.add(queryRes, IPROP.timeOuts, ResourceFactory.createTypedLiteral(timeOuts));
        m.add(queryRes, IPROP.wrongCodes, ResourceFactory.createTypedLiteral(wrongCodes));
        m.add(queryRes, IPROP.unknownException, ResourceFactory.createTypedLiteral(unknownExceptions));
        m.add(queryRes, IPROP.totalTime, ResourceFactory.createTypedLiteral(toXSDDurationInSeconds(totalTime)));
        m.add(queryRes, RDF.type, IONT.executedQuery);

        return m;
    }
}
