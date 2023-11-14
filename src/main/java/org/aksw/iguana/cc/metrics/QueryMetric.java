package org.aksw.iguana.cc.metrics;

import org.aksw.iguana.cc.worker.HttpWorker;

import java.util.List;

public interface QueryMetric {
    Number calculateQueryMetric(List<HttpWorker.ExecutionStats> data);
}
