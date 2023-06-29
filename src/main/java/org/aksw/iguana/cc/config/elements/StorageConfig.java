package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.commons.factory.TypedFactory;
import org.aksw.iguana.rp.storage.Storage;

import java.util.HashMap;
import java.util.Map;

/**
 * Storage Configuration class
 */
public class StorageConfig {


    @JsonProperty(required = true)
    private String className;

    @JsonProperty
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

    public Storage createStorage() {
        TypedFactory<Storage> factory = new TypedFactory<>();
        return factory.create(className, configuration);
    }
}
