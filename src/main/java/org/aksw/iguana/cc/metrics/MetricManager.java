package org.aksw.iguana.cc.metrics;

import java.util.List;

public class MetricManager {
    private static List<Metric> metrics;

    public static void setMetrics(List<Metric> metrics) {
        MetricManager.metrics = metrics;
    }

    public static List<Metric> getMetrics() {
        return MetricManager.metrics;
    }
}
