package org.aksw.iguana.cc.metrics;

import org.aksw.iguana.cc.worker.HttpWorker;

import java.util.List;

public interface TaskMetric {
    Number calculateTaskMetric(List<HttpWorker> workers, List<HttpWorker.ExecutionStats>[][] data);
}
