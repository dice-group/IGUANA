package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The task configuration class, sets the class name and it's configuration
 */
public class Task implements Serializable {

    @JsonProperty(required = true)
    private Map configuration = new HashMap();

    @JsonProperty(required = true)
    private String className;

    @JsonProperty(required = false)
    private String name=null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
