package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.cc.tasks.stresstest.metrics.Metric;
import org.aksw.iguana.commons.factory.TypedFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Metric Config class
 */
public class MetricConfig {

    @JsonProperty(required = true)
    private String className;

    @JsonProperty()
    private Map<String, Object> configuration = new HashMap<>();


    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public Metric createMetric() {
        TypedFactory<Metric> factory = new TypedFactory<>();
        return factory.create(className, configuration);
    }
}
