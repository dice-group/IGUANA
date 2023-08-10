package org.aksw.iguana.cc.metrics.impl;

import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.metrics.QueryMetric;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.commons.annotation.Shorthand;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Shorthand("PQPS")
public class PQPS extends Metric implements QueryMetric {

    private final int penalty;

    public PQPS(Integer penalty) {
        super("Penalized Queries per Second", "PQPS", "This metric calculates for each query the amount of executions per second. Failed executions receive a time penalty.");
        this.penalty = penalty;
    }

    @Override
    public Number calculateQueryMetric(List<HttpWorker.ExecutionStats> data) {
        BigDecimal successes = BigDecimal.ZERO;
        Duration totalTime = Duration.ZERO;
        for (HttpWorker.ExecutionStats exec : data) {
            successes = successes.add(BigDecimal.ONE);
            if (exec.successful()) {
                totalTime = totalTime.plus(exec.duration());
            } else {
                totalTime = totalTime.plusMillis(penalty);
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
