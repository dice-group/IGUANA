package org.aksw.iguana.cc.tasks.stresstest.metrics.impl;

import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.tasks.stresstest.StresstestMetadata;
import org.aksw.iguana.cc.worker.WorkerMetadata;
import org.aksw.iguana.cc.tasks.stresstest.metrics.Metric;
import org.aksw.iguana.cc.tasks.stresstest.metrics.TaskMetric;
import org.aksw.iguana.cc.tasks.stresstest.metrics.WorkerMetric;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;

import java.math.BigInteger;
import java.util.List;

@Shorthand("NoQ")
public class NoQ extends Metric implements TaskMetric, WorkerMetric {

    public NoQ() {
        super("Number of Queries", "NoQ", "This metric calculates the number of successfully executed queries.");
    }

    @Override
    public Number calculateTaskMetric(StresstestMetadata task, List<QueryExecutionStats>[][] data) {
        BigInteger sum = BigInteger.ZERO;
        for (WorkerMetadata worker : task.workers()) {
            sum = sum.add((BigInteger) this.calculateWorkerMetric(worker, data[worker.workerID()]));
        }
        return sum;
    }

    @Override
    public Number calculateWorkerMetric(WorkerMetadata worker, List<QueryExecutionStats>[] data) {
        BigInteger sum = BigInteger.ZERO;
        for (List<QueryExecutionStats> datum : data) {
            for (QueryExecutionStats exec : datum) {
                if (exec.responseCode() == COMMON.QUERY_SUCCESS) {
                    sum = sum.add(BigInteger.ONE);
                }
            }
        }
        return sum;
    }
}
