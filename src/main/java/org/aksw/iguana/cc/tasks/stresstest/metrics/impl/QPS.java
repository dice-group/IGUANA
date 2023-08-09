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

@Shorthand("QPS")
public class QPS extends Metric implements QueryMetric {

    public QPS() {
        super("Queries per Second", "QPS", "This metric calculates for each query the amount of executions per second.");
    }

    @Override
    public Number calculateQueryMetric(List<QueryExecutionStats> data) {
        BigDecimal successes = BigDecimal.ZERO;
        Duration totalTime = Duration.ZERO;
        for (QueryExecutionStats exec : data) {
            if (exec.responseCode() == COMMON.QUERY_SUCCESS) {
                successes = successes.add(BigDecimal.ONE);
                totalTime = totalTime.plusNanos((long) exec.executionTime() * 1000000);
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
