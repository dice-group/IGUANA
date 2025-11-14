package org.aksw.iguana.cc.metrics.impl;

import org.aksw.iguana.cc.metrics.MetricTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QPSTest extends MetricTest {

    @Test
    public void testQPSQueryMetric() {
        final var testData = queryResults(0, List.of(
                Duration.ofMillis(500),
                Duration.ofMillis(1000),
                Duration.ofMillis(1500),
                Duration.ofMillis(100)
        ), 2, 1, 1);
        final var metric = new QPS();
        final var actual = metric.calculateQueryMetric(testData);
        final var expected = new java.math.BigDecimal("1");

        assertEquals(0, expected.compareTo((BigDecimal) actual));
    }

}