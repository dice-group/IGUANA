package org.aksw.iguana.cc.metrics;

import org.aksw.iguana.cc.worker.HttpWorker;

import java.util.List;

public interface WorkerMetric {
    Number calculateWorkerMetric(HttpWorker.Config worker, List<HttpWorker.ExecutionStats>[] data);
}
