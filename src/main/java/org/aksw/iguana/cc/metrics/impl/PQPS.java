package org.aksw.iguana.cc.metrics.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.metrics.QueryMetric;
import org.aksw.iguana.cc.worker.HttpWorker;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

public class PQPS extends Metric implements QueryMetric {

    private final int penalty;

    public PQPS(@JsonProperty("penalty") Integer penalty) {
        super("Penalized Queries per Second", "PQPS", "This metric calculates for each query the amount of executions per second. Failed executions receive a time penalty.");
        this.penalty = penalty;
    }

    @Override
    public Number calculateQueryMetric(List<HttpWorker.ExecutionStats> data) {
        BigDecimal numberOfExecutions = BigDecimal.ZERO;
        Duration totalTime = Duration.ZERO;
        for (HttpWorker.ExecutionStats exec : data) {
            numberOfExecutions = numberOfExecutions.add(BigDecimal.ONE);
            if (exec.successful()) {
                totalTime = totalTime.plus(exec.duration());
            } else {
                totalTime = totalTime.plusMillis(penalty);
            }
        }
        BigDecimal tt = (new BigDecimal(BigInteger.valueOf(totalTime.toNanos()), 9));

        try {
            return numberOfExecutions.divide(tt, 10, RoundingMode.HALF_UP);
        } catch (ArithmeticException e) {
            return BigDecimal.ZERO;
        }
    }
}
