package org.aksw.iguana.cc.tasks.stresstest.metrics;

import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.model.WorkerMetadata;

import java.util.List;

public interface WorkerMetric {
    Number calculateWorkerMetric(WorkerMetadata worker, List<QueryExecutionStats>[] data);
}
