package org.aksw.iguana.cc.metrics.impl;

import org.aksw.iguana.cc.metrics.MetricTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoQPHTest extends MetricTest {

    @Test
    void testNoQPHTaskMetric() {
        // query 0 with 3 successful and timed out executions for a duration of 3s
        final var testData0 = queryResults(0, List.of(
                Duration.ofMillis(500),  // successful
                Duration.ofMillis(1000), // successful
                Duration.ofMillis(1500), // timeout
                Duration.ofMillis(100)   // general error
        ), 2, 1, 1);

        // query 1 with 3 successful and timed out executions for a duration of 9s
        final var testData1 = queryResults(1, List.of(
                Duration.ofMillis(1500), // successful
                Duration.ofMillis(3000), // successful
                Duration.ofMillis(4500), // timeout
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
        final var metric = new NoQPH();
        final var workerConfig0 = getWorker(0);
        final var workerConfig1 = getWorker(1);
        final var actual = metric.calculateTaskMetric(List.of(workerConfig0, workerConfig1), dataArray);
        final var expected = new java.math.BigDecimal("3600"); // 6 executions with total time of 12 s per worker

        assertEquals(0, expected.compareTo((BigDecimal) actual));
    }

    @Test
    void testNoQPHWorkerMetric() {
        // query 0 with 3 successful and timed out executions for a duration of 3s
        final var testData0 = queryResults(0, List.of(
                Duration.ofMillis(500),  // successful
                Duration.ofMillis(1000), // successful
                Duration.ofMillis(1500), // timeout
                Duration.ofMillis(100)   // general error
        ), 2, 1, 1);

        // query 1 with 3 successful and timed out executions for a duration of 9s
        final var testData1 = queryResults(1, List.of(
                Duration.ofMillis(1500), // successful
                Duration.ofMillis(3000), // successful
                Duration.ofMillis(4500), // timeout
                Duration.ofMillis(300)   // general error
        ), 2, 1, 1);
        final var dataArray = new List[]{
                testData0,
                testData1
        };
        final var metric = new NoQPH();
        final var worker = getWorker(0);
        final var actual = metric.calculateWorkerMetric(worker.config(), dataArray);
        final var expected = new java.math.BigDecimal("1800"); // 6 executions with total time of 12 s

        assertEquals(0, expected.compareTo((BigDecimal) actual));
    }
}
