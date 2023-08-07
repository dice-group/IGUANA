package org.aksw.iguana.cc.tasks.stresstest.metrics;

import org.aksw.iguana.cc.model.QueryExecutionStats;

import java.util.List;

public interface QueryMetric {
    Number calculateQueryMetric(List<QueryExecutionStats> data);
}
