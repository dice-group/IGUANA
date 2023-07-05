package org.aksw.iguana.cc.tasks.stresstest.metrics;

import org.aksw.iguana.cc.model.QueryExecutionStats;
import org.aksw.iguana.cc.tasks.stresstest.StresstestMetadata;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public interface ModelWritingMetric {
    default @Nonnull Model createMetricModel(StresstestMetadata task, List<QueryExecutionStats>[][] data) {
        return ModelFactory.createDefaultModel();
    }

    default @Nonnull Model createMetricModel(StresstestMetadata task, Map<String, List<QueryExecutionStats>> data) {
        return ModelFactory.createDefaultModel();
    }
}
