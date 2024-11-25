package org.dice_research.iguana.cc.metrics;

import org.dice_research.iguana.cc.worker.HttpWorker;

import java.util.List;

public interface WorkerMetric {
    Number calculateWorkerMetric(HttpWorker.Config worker, List<HttpWorker.ExecutionStats>[] data);
}
