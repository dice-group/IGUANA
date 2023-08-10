package org.aksw.iguana.cc.metrics.impl;

import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.metrics.TaskMetric;
import org.aksw.iguana.cc.metrics.WorkerMetric;
import org.aksw.iguana.cc.worker.HttpWorker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class AvgQPS extends Metric implements TaskMetric, WorkerMetric {

    public AvgQPS() {
        super("Average Queries per Second", "AvgQPS", "This metric calculates the average QPS between all queries.");
    }

    @Override
    public Number calculateTaskMetric(List<HttpWorker> workers, List<HttpWorker.ExecutionStats>[][] data) {
        BigDecimal sum = BigDecimal.ZERO;
        for (var worker : workers) {
            sum = sum.add((BigDecimal) this.calculateWorkerMetric(worker.config(), data[(int) worker.getWorkerID()]));
        }

        try {
            return sum.divide(BigDecimal.valueOf(data.length), 10, RoundingMode.HALF_UP);
        } catch (ArithmeticException e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public Number calculateWorkerMetric(HttpWorker.Config worker, List<HttpWorker.ExecutionStats>[] data) {
        BigDecimal sum = BigDecimal.ZERO;
        QPS qpsmetric = new QPS();
        for (List<HttpWorker.ExecutionStats> datum : data) {
            sum = sum.add((BigDecimal) qpsmetric.calculateQueryMetric(datum));
        }

        try {
            return sum.divide(BigDecimal.valueOf(data.length), 10, RoundingMode.HALF_UP);
        } catch (ArithmeticException e) {
            return BigDecimal.ZERO;
        }
    }
}
