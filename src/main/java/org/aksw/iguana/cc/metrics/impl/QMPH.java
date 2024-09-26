package org.aksw.iguana.cc.metrics.impl;

import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.metrics.TaskMetric;
import org.aksw.iguana.cc.metrics.WorkerMetric;
import org.aksw.iguana.cc.worker.HttpWorker;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

public class QMPH extends Metric implements TaskMetric, WorkerMetric {

    public QMPH() {
        super("Query Mixes per Hour", "QMPH", "This metric calculates the amount of query mixes (a given set of queries) that are executed per hour.");
    }

    @Override
    public Number calculateTaskMetric(List<HttpWorker> workers, List<HttpWorker.ExecutionStats>[][] data) {
        final var sum = workers.stream()
                .map(worker -> (BigDecimal) this.calculateWorkerMetric(worker.config(), data[(int) worker.getWorkerID()]))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum;
    }

    @Override
    public Number calculateWorkerMetric(HttpWorker.Config worker, List<HttpWorker.ExecutionStats>[] data) {
        BigDecimal successes = BigDecimal.ZERO;
        BigDecimal noq = BigDecimal.valueOf(worker.queries().getExecutableQueryCount());
        Duration totalTime = Duration.ZERO;
        for (List<HttpWorker.ExecutionStats> datum : data) {
            for (HttpWorker.ExecutionStats exec : datum) {
                if (exec.successful()) {
                    successes = successes.add(BigDecimal.ONE);
                    totalTime = totalTime.plus(exec.duration());
                }
            }
        }
        BigDecimal tt = (new BigDecimal(BigInteger.valueOf(totalTime.toNanos()), 9)).divide(BigDecimal.valueOf(3600), 20, RoundingMode.HALF_UP);

        try {
            return successes.divide(tt, 10, RoundingMode.HALF_UP).divide(noq, 10, RoundingMode.HALF_UP);
        } catch (ArithmeticException e) {
            return BigDecimal.ZERO;
        }
    }
}
