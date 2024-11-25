package org.dice_research.iguana.cc.metrics;

import org.dice_research.iguana.cc.worker.HttpWorker;

import java.util.List;

public interface QueryMetric {
    Number calculateQueryMetric(List<HttpWorker.ExecutionStats> data);
}
