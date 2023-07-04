package org.aksw.iguana.cc.tasks.stresstest.metrics.impl;

import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.model.StresstestMetadata;
import org.aksw.iguana.cc.model.WorkerMetadata;
import org.aksw.iguana.cc.tasks.stresstest.metrics.Metric;
import org.aksw.iguana.cc.tasks.stresstest.metrics.TaskMetric;
import org.aksw.iguana.cc.tasks.stresstest.metrics.WorkerMetric;
import org.aksw.iguana.commons.annotation.Shorthand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Shorthand("PAvgQPS")
public class PAvgQPS extends Metric implements TaskMetric, WorkerMetric {

    private final long penalty;

    public PAvgQPS(long penalty) {
        super("Penalized Average Queries per Second", "AvgQPS", "This metric calculates the average QPS between all queries. Failed executions receive a time penalty.");
        this.penalty = penalty;
    }

    @Override
    public Number calculateTaskMetric(StresstestMetadata task, List<QueryExecutionStats>[][] data) {
        BigDecimal sum = BigDecimal.ZERO;
        for (WorkerMetadata worker : task.workers()) {
            sum = sum.add((BigDecimal) this.calculateWorkerMetric(worker, data[worker.workerID()]));
        }

        try {
            return sum.divide(BigDecimal.valueOf(data.length), 10, RoundingMode.HALF_UP);
        } catch (ArithmeticException e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public Number calculateWorkerMetric(WorkerMetadata worker, List<QueryExecutionStats>[] data) {
        BigDecimal sum = BigDecimal.ZERO;
        PQPS pqpsmetric = new PQPS(penalty);
        for (List<QueryExecutionStats> datum : data) {
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
