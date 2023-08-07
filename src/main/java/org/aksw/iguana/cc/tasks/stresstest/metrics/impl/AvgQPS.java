package org.aksw.iguana.cc.tasks.stresstest.metrics.impl;

import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.tasks.stresstest.StresstestMetadata;
import org.aksw.iguana.cc.worker.WorkerMetadata;
import org.aksw.iguana.cc.tasks.stresstest.metrics.Metric;
import org.aksw.iguana.cc.tasks.stresstest.metrics.TaskMetric;
import org.aksw.iguana.cc.tasks.stresstest.metrics.WorkerMetric;
import org.aksw.iguana.commons.annotation.Shorthand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Shorthand("AvgQPS")
public class AvgQPS extends Metric implements TaskMetric, WorkerMetric {

    public AvgQPS() {
        super("Average Queries per Second", "AvgQPS", "This metric calculates the average QPS between all queries.");
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
        QPS qpsmetric = new QPS();
        for (List<QueryExecutionStats> datum : data) {
            sum = sum.add((BigDecimal) qpsmetric.calculateQueryMetric(datum));
        }

        try {
            return sum.divide(BigDecimal.valueOf(data.length), 10, RoundingMode.HALF_UP);
        } catch (ArithmeticException e) {
            return BigDecimal.ZERO;
        }
    }
}
