package org.aksw.iguana.cc.metrics.impl;

import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.metrics.TaskMetric;
import org.aksw.iguana.cc.metrics.WorkerMetric;
import org.aksw.iguana.cc.worker.HttpWorker;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class NoQ extends Metric implements TaskMetric, WorkerMetric {

    public NoQ() {
        super("Number of Queries", "NoQ", "This metric calculates the number of successfully executed queries.");
    }

    @Override
    public Number calculateTaskMetric(List<HttpWorker> workers, List<HttpWorker.ExecutionStats>[][] data) {
        final var sum = workers.stream()
                .map(worker -> (BigInteger) this.calculateWorkerMetric(worker.config(), data[(int) worker.getWorkerID()]))
                .reduce(BigInteger.ZERO, BigInteger::add);
        return sum;
    }

    @Override
    public Number calculateWorkerMetric(HttpWorker.Config worker, List<HttpWorker.ExecutionStats>[] data) {
        BigInteger sum = BigInteger.ZERO;
        for (List<HttpWorker.ExecutionStats> datum : data) {
            for (HttpWorker.ExecutionStats exec : datum) {
                if (exec.successful()) {
                    sum = sum.add(BigInteger.ONE);
                }
            }
        }
        return sum;
    }
}
