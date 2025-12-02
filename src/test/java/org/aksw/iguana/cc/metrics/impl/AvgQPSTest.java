package org.aksw.iguana.cc.metrics.impl;

import org.aksw.iguana.cc.metrics.MetricTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AvgQPSTest extends MetricTest {

    @Test
    void testAvgQPSTaskMetric() {
        // query 0 with qps of 1.5
        final var testData0 = queryResults(0, List.of(
                Duration.ofMillis(500),  // successful
                Duration.ofMillis(500),  // successful
                Duration.ofMillis(1000), // timeout
                Duration.ofMillis(100)   // general error
        ), 2, 1, 1);

        // query 1 with qps of 0.5
        final var testData1 = queryResults(1, List.of(
                Duration.ofMillis(1500), // successful
                Duration.ofMillis(1500), // successful
                Duration.ofMillis(3000), // timeout
                Duration.ofMillis(300)   // general error
        ), 2, 1, 1);
        final var dataArrayWorker0 = new List[]{
                testData0,
                testData1
        };
        final var dataArrayWorker1 = new List[]{
                testData0,
                testData1
        };
        final var dataArray = new List[][]{
                dataArrayWorker0,
                dataArrayWorker1
        };
        final var metric = new AvgQPS();
        final var workerConfig0 = getWorker(0);
        final var workerConfig1 = getWorker(1);
        final var actual = metric.calculateTaskMetric(List.of(workerConfig0, workerConfig1), dataArray);
        final var expected = new java.math.BigDecimal("1"); // 1 avgqps

        assertEquals(0, expected.compareTo((BigDecimal) actual));
    }

    @Test
    void testAvgQPSWorkerMetric() {
        // query 0 with qps of 1.5
        final var testData0 = queryResults(0, List.of(
                Duration.ofMillis(500),  // successful
                Duration.ofMillis(500),  // successful
                Duration.ofMillis(1000), // timeout
                Duration.ofMillis(100)   // general error
        ), 2, 1, 1);

        // query 1 with qps of 0.5
        final var testData1 = queryResults(1, List.of(
                Duration.ofMillis(1500), // successful
                Duration.ofMillis(1500), // successful
                Duration.ofMillis(3000), // timeout
                Duration.ofMillis(300)   // general error
        ), 2, 1, 1);
        final var dataArray = new List[]{
                testData0,
                testData1
        };
        final var metric = new AvgQPS();
        final var worker = getWorker(0);
        final var actual = metric.calculateWorkerMetric(worker.config(), dataArray);
        final var expected = new java.math.BigDecimal("1"); // avg qps of all workers

        assertEquals(0, expected.compareTo((BigDecimal) actual));
    }
}
