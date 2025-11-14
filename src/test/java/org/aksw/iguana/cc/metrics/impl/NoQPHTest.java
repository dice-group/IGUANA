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
        final var testData0 = queryResults(0, List.of(
                Duration.ofMillis(500),
                Duration.ofMillis(1000),
                Duration.ofMillis(1500),
                Duration.ofMillis(100)
        ), 2, 1, 1);
        final var testData1 = queryResults(1, List.of(
                Duration.ofMillis(1500),
                Duration.ofMillis(3000),
                Duration.ofMillis(4500),
                Duration.ofMillis(300)
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
        final var expected = new java.math.BigDecimal("3600");

        assertEquals(0, expected.compareTo((BigDecimal) actual));
    }

    @Test
    void testNoQPHWorkerMetric() {
        final var testData0 = queryResults(0, List.of(
                Duration.ofMillis(500),
                Duration.ofMillis(1000),
                Duration.ofMillis(1500),
                Duration.ofMillis(100)
        ), 2, 1, 1);
        final var testData1 = queryResults(1, List.of(
                Duration.ofMillis(1500),
                Duration.ofMillis(3000),
                Duration.ofMillis(4500),
                Duration.ofMillis(300)
        ), 2, 1, 1);
        final var dataArray = new List[]{
                testData0,
                testData1
        };
        final var metric = new NoQPH();
        final var worker = getWorker(0);
        final var actual = metric.calculateWorkerMetric(worker.config(), dataArray);
        final var expected = new java.math.BigDecimal("1800");

        assertEquals(0, expected.compareTo((BigDecimal) actual));
    }
}
