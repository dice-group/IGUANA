package org.aksw.iguana.cc.metrics;

import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.commons.rdf.IRES;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.util.List;
import java.util.Map;

public interface ModelWritingMetric {
    default Model createMetricModel(List<HttpWorker> workers, List<HttpWorker.ExecutionStats>[][] data, IRES.Factory iresFactory) {
        return ModelFactory.createDefaultModel();
    }

    default Model createMetricModel(List<HttpWorker> workers, Map<String, List<HttpWorker.ExecutionStats>> data, IRES.Factory iresFactory) {
        return ModelFactory.createDefaultModel();
    }
}
