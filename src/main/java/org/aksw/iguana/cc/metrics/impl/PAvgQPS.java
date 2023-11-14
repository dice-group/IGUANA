package org.aksw.iguana.cc.metrics.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.metrics.TaskMetric;
import org.aksw.iguana.cc.metrics.WorkerMetric;
import org.aksw.iguana.cc.worker.HttpWorker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PAvgQPS extends Metric implements TaskMetric, WorkerMetric {

    private final int penalty;

    public PAvgQPS(@JsonProperty("penalty") Integer penalty) {
        super("Penalized Average Queries per Second", "PAvgQPS", "This metric calculates the average QPS between all queries. Failed executions receive a time penalty.");
        this.penalty = penalty;
    }

    @Override
    public Number calculateTaskMetric(List<HttpWorker> workers, List<HttpWorker.ExecutionStats>[][] data) {
        final var sum = workers.stream()
                .map(worker -> (BigDecimal) this.calculateWorkerMetric(worker.config(), data[(int) worker.getWorkerID()]))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try {
            return sum.divide(BigDecimal.valueOf(data.length), 10, RoundingMode.HALF_UP);
        } catch (ArithmeticException e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public Number calculateWorkerMetric(HttpWorker.Config worker, List<HttpWorker.ExecutionStats>[] data) {
        BigDecimal sum = BigDecimal.ZERO;
        PQPS pqpsmetric = new PQPS(penalty);
        for (List<HttpWorker.ExecutionStats> datum : data) {
            sum = sum.add((BigDecimal) pqpsmetric.calculateQueryMetric(datum));
        }
        if (data.length == 0) {
            return BigDecimal.ZERO;
        }

        try {
            return sum.divide(BigDecimal.valueOf(data.length), 10, RoundingMode.HALF_UP);
        } catch (ArithmeticException e) {
            return BigDecimal.ZERO;
        }
    }
}
