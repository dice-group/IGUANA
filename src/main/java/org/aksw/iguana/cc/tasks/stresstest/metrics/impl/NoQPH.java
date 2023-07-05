package org.aksw.iguana.cc.tasks.stresstest.metrics.impl;

import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.tasks.stresstest.StresstestMetadata;
import org.aksw.iguana.cc.worker.WorkerMetadata;
import org.aksw.iguana.cc.tasks.stresstest.metrics.Metric;
import org.aksw.iguana.cc.tasks.stresstest.metrics.TaskMetric;
import org.aksw.iguana.cc.tasks.stresstest.metrics.WorkerMetric;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Shorthand("NoQPH")
public class NoQPH extends Metric implements TaskMetric, WorkerMetric {

    public NoQPH() {
        super("Number of Queries per Hour", "NoQPH", "This metric calculates the number of successfully executed queries per hour.");
    }
    @Override
    public Number calculateTaskMetric(StresstestMetadata task, List<QueryExecutionStats>[][] data) {
        BigDecimal sum = BigDecimal.ZERO;
        for (WorkerMetadata worker : task.workers()) {
            sum = sum.add((BigDecimal) this.calculateWorkerMetric(worker, data[worker.workerID()]));
        }
        return sum;
    }

    @Override
    public Number calculateWorkerMetric(WorkerMetadata worker, List<QueryExecutionStats>[] data) {
        BigDecimal successes = BigDecimal.ZERO;
        Duration totalTime = Duration.ZERO;
        for (List<QueryExecutionStats> datum : data) {
            for (QueryExecutionStats exec : datum) {
                if (exec.responseCode() == COMMON.QUERY_SUCCESS) {
                    successes = successes.add(BigDecimal.ONE);
                    totalTime = totalTime.plusNanos((long) exec.executionTime() * 1000000);
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
