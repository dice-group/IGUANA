package org.aksw.iguana.cc.metrics.impl;

import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.metrics.ModelWritingMetric;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.commons.rdf.IONT;
import org.aksw.iguana.commons.rdf.IPROP;
import org.aksw.iguana.commons.rdf.IRES;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.aksw.iguana.commons.time.TimeUtils.toXSDDurationInSeconds;

public class AggregatedExecutionStatistics extends Metric implements ModelWritingMetric {

    public AggregatedExecutionStatistics() {
        super("Aggregated Execution Statistics", "AES", "Sums up the statistics of each query execution for each query a worker and task has. The result size only contains the value of the last execution.");
    }

    @Override
    @NotNull
    public Model createMetricModel(List<HttpWorker> workers, List<HttpWorker.ExecutionStats>[][] data, IRES.Factory iresFactory) {
        Model m = ModelFactory.createDefaultModel();
        for (var worker : workers) {
            for (int i = 0; i < worker.config().queries().getQueryCount(); i++) {
                Resource queryRes = iresFactory.getWorkerQueryResource(worker, i);
                m.add(createAggregatedModel(data[(int) worker.getWorkerID()][i], queryRes));
            }
        }
        return m;
    }

    @Override
    @NotNull
    public Model createMetricModel(List<HttpWorker> workers, Map<String, List<HttpWorker.ExecutionStats>> data, IRES.Factory iresFactory) {
        Model m = ModelFactory.createDefaultModel();
        for (String queryID : data.keySet()) {
            Resource queryRes = iresFactory.getTaskQueryResource(queryID);
            m.add(createAggregatedModel(data.get(queryID), queryRes));
        }
        return m;
    }

    private static Model createAggregatedModel(List<HttpWorker.ExecutionStats> data, Resource queryRes) {
        Model m = ModelFactory.createDefaultModel();
        BigInteger succeeded = BigInteger.ZERO;
        BigInteger failed = BigInteger.ZERO;
        Optional<BigInteger> resultSize = Optional.empty();
        BigInteger wrongCodes = BigInteger.ZERO;
        BigInteger timeOuts = BigInteger.ZERO;
        BigInteger unknownExceptions = BigInteger.ZERO;
        Duration totalTime = Duration.ZERO;

        for (HttpWorker.ExecutionStats exec : data) {
            switch (exec.endState()) {
                case SUCCESS -> succeeded = succeeded.add(BigInteger.ONE);
                case TIMEOUT -> timeOuts = timeOuts.add(BigInteger.ONE);
                case HTTP_ERROR -> wrongCodes = wrongCodes.add(BigInteger.ONE);
                case MISCELLANEOUS_EXCEPTION -> unknownExceptions = unknownExceptions.add(BigInteger.ONE);
            }

            if (!exec.successful())
                failed = failed.add(BigInteger.ONE);

            totalTime = totalTime.plus(exec.duration());
            if (exec.contentLength().isPresent())
                resultSize = Optional.of(BigInteger.valueOf(exec.contentLength().getAsLong()));
        }

        m.add(queryRes, IPROP.succeeded, ResourceFactory.createTypedLiteral(succeeded));
        m.add(queryRes, IPROP.failed, ResourceFactory.createTypedLiteral(failed));
        m.add(queryRes, IPROP.resultSize, ResourceFactory.createTypedLiteral(resultSize.orElse(BigInteger.valueOf(-1))));
        m.add(queryRes, IPROP.timeOuts, ResourceFactory.createTypedLiteral(timeOuts));
        m.add(queryRes, IPROP.wrongCodes, ResourceFactory.createTypedLiteral(wrongCodes));
        m.add(queryRes, IPROP.unknownException, ResourceFactory.createTypedLiteral(unknownExceptions));
        m.add(queryRes, IPROP.totalTime, ResourceFactory.createTypedLiteral(toXSDDurationInSeconds(totalTime)));
        m.add(queryRes, RDF.type, IONT.executedQuery);

        return m;
    }
}
