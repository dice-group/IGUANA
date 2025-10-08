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

public class PQMPH extends Metric implements TaskMetric, WorkerMetric {

    private final int penalty;

    public PQMPH(@JsonProperty("penalty") Integer penalty) {
        super("Penalized Query Mixes per Hour", "PQMPH", "This metric calculates the amount of query mixes (a given set of queries) that are executed per hour. Failed executions receive a time penalty.");
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
        BigDecimal executions = BigDecimal.ZERO;
        BigDecimal noq = BigDecimal.valueOf(worker.queries().getExecutableQueryCount());
        Duration totalTime = Duration.ZERO;
        for (List<HttpWorker.ExecutionStats> datum : data) {
            for (HttpWorker.ExecutionStats exec : datum) {
                if (exec.successful()) {
                    executions = executions.add(BigDecimal.ONE);
                    totalTime = totalTime.plus(exec.duration());
                } else {
                    totalTime = totalTime.plusMillis(penalty);
                }
            }
        }

        BigDecimal totalTimeBigDecimal = new BigDecimal(BigInteger.valueOf(totalTime.toNanos()), 9);

        BigDecimal queriesPerHour = executions.divide(totalTimeBigDecimal, 10, RoundingMode.HALF_UP) // QPH = QPS * 3600
                                              .multiply(BigDecimal.valueOf(3600));
        try {
            return queriesPerHour.divide(noq, 10, RoundingMode.HALF_UP).stripTrailingZeros(); // Convert QPH to QMPH
        } catch (ArithmeticException e) {
            return BigDecimal.ZERO;
        }
    }
}
