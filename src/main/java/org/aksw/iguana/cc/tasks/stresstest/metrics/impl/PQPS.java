package org.aksw.iguana.cc.tasks.stresstest.metrics.impl;

import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.tasks.stresstest.metrics.Metric;
import org.aksw.iguana.cc.tasks.stresstest.metrics.QueryMetric;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Shorthand("PQPS")
public class PQPS extends Metric implements QueryMetric {

    private final long penalty;

    public PQPS(long penalty) {
        super("Penalized Queries per Second", "PQPS", "This metric calculates for each query the amount of executions per second. Failed executions receive a time penalty.");
        this.penalty = penalty;
    }

    @Override
    public Number calculateQueryMetric(List<QueryExecutionStats> data) {
        BigDecimal successes = BigDecimal.ZERO;
        Duration totalTime = Duration.ZERO;
        for (QueryExecutionStats exec : data) {
            successes = successes.add(BigDecimal.ONE);
            if (exec.responseCode() == COMMON.QUERY_SUCCESS) {
                totalTime = totalTime.plusNanos((long) exec.executionTime() * 1000000);
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
