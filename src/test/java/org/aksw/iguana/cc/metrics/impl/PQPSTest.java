package org.aksw.iguana.cc.metrics.impl;

import org.aksw.iguana.cc.metrics.MetricTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PQPSTest extends MetricTest {

    @Test
    public void testPQPSQueryMetric() {
        final var testData = queryResults(0, List.of(
                Duration.ofMillis(500),
                Duration.ofMillis(1500),
                Duration.ofMillis(1500),
                Duration.ofMillis(100)
        ), 2, 1, 1);
        final var metric = new PQPS(3000);
        final var actual = metric.calculateQueryMetric(testData);
        final var expected = new java.math.BigDecimal("0.5");


        assertEquals(0, expected.compareTo((BigDecimal) actual));
    }
}
