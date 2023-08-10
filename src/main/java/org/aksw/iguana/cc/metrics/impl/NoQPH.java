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

public class NoQPH extends Metric implements TaskMetric, WorkerMetric {

    public NoQPH() {
        super("Number of Queries per Hour", "NoQPH", "This metric calculates the number of successfully executed queries per hour.");
    }
    @Override
    public Number calculateTaskMetric(List<HttpWorker> workers, List<HttpWorker.ExecutionStats>[][] data) {
        BigDecimal sum = BigDecimal.ZERO;
        for (var worker : workers) {
            sum = sum.add((BigDecimal) this.calculateWorkerMetric(worker.config(), data[(int) worker.getWorkerID()]));
        }
        return sum;
    }

    @Override
    public Number calculateWorkerMetric(HttpWorker.Config worker, List<HttpWorker.ExecutionStats>[] data) {
        BigDecimal successes = BigDecimal.ZERO;
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
            return successes.divide(tt, 10, RoundingMode.HALF_UP);
        } catch (ArithmeticException e) {
            return BigDecimal.ZERO;
        }
    }
}
