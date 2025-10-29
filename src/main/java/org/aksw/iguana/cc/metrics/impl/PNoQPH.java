package org.aksw.iguana.cc.metrics.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.metrics.TaskMetric;
import org.aksw.iguana.cc.metrics.WorkerMetric;
import org.aksw.iguana.cc.worker.HttpWorker;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

public class PNoQPH extends Metric implements TaskMetric, WorkerMetric {

    private final int penalty;

    public PNoQPH(@JsonProperty("penalty") Integer penalty) {
        super("Penalized Number of Queries per Hour", "PNoQPH", "This metric calculates the number of executed queries per hour. Failed executions receive a time penalty.");
        this.penalty = penalty;
    }
    @Override
    public Number calculateTaskMetric(List<HttpWorker> workers, List<HttpWorker.ExecutionStats>[][] data) {
        final var sum = workers.stream()
                .map(worker -> (BigDecimal) this.calculateWorkerMetric(worker.config(), data[(int) worker.getWorkerID()]))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.stripTrailingZeros();
    }

    @Override
    public Number calculateWorkerMetric(HttpWorker.Config worker, List<HttpWorker.ExecutionStats>[] data) {
        BigDecimal executions = BigDecimal.valueOf(data.length);
        Duration totalTime = Duration.ZERO;
        for (List<HttpWorker.ExecutionStats> datum : data) {
            for (HttpWorker.ExecutionStats exec : datum) {
                if (exec.successful()) {
                    totalTime = totalTime.plus(exec.duration());
                }
                else {
                    totalTime = totalTime.plusMillis(penalty);
                }
            }
        }
        BigDecimal tt = (new BigDecimal(BigInteger.valueOf(totalTime.toNanos()), 9)).divide(BigDecimal.valueOf(3600), 20, RoundingMode.HALF_UP);

        try {
            return executions.divide(tt, 10, RoundingMode.HALF_UP).stripTrailingZeros();
        } catch (ArithmeticException e) {
            return BigDecimal.ZERO;
        }
    }
}
