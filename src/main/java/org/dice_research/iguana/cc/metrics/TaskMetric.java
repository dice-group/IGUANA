package org.dice_research.iguana.cc.metrics;

import org.dice_research.iguana.cc.worker.HttpWorker;

import java.util.List;

public interface TaskMetric {
    Number calculateTaskMetric(List<HttpWorker> workers, List<HttpWorker.ExecutionStats>[][] data);
}
