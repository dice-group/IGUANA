package org.aksw.iguana.cc.metrics.impl;

import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.metrics.QueryMetric;
import org.aksw.iguana.cc.worker.HttpWorker;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

public class QPS extends Metric implements QueryMetric {

    public QPS() {
        super("Queries per Second", "QPS", "This metric calculates for each query the amount of executions per second.");
    }

    @Override
    public Number calculateQueryMetric(List<HttpWorker.ExecutionStats> data) {
        BigDecimal successes = BigDecimal.ZERO;
        Duration totalTime = Duration.ZERO;
        for (HttpWorker.ExecutionStats exec : data) {
            if (exec.successful()) {
                successes = successes.add(BigDecimal.ONE);
                totalTime = totalTime.plus(exec.duration());
            }
        }
        BigDecimal tt = (new BigDecimal(BigInteger.valueOf(totalTime.toNanos()), 9));
        try {
            return successes.divide(tt, 10, RoundingMode.HALF_UP);
        } catch (ArithmeticException e) {
            return BigDecimal.ZERO;
        }
    }
}
