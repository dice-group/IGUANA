package org.aksw.iguana.cc.tasks.stresstest.metrics;

import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.model.StresstestMetadata;

import java.util.List;

public interface TaskMetric {
    Number calculateTaskMetric(StresstestMetadata task, List<QueryExecutionStats>[][] data);
}
