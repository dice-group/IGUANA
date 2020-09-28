package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.commons.factory.TypedFactory;
import org.aksw.iguana.rp.metrics.Metric;

import java.util.HashMap;
import java.util.Map;

/**
 * Metric Config class
 */
public class MetricConfig {

    @JsonProperty(required = true)
    private String className;

    @JsonProperty(required = false)
    private Map configuration = new HashMap();


    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Map getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map configuration) {
        this.configuration = configuration;
    }

    public Metric createMetric() {
        TypedFactory<Metric> factory = new TypedFactory<Metric>();
        return factory.create(className, configuration);
    }
}
