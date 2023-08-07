package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Dataset config class.
 *
 * Will set the name and if it was set in the config file the fileName
 */
public class DatasetConfig {
    @JsonProperty(required = true)
    private String name;

    @JsonProperty
    private String file;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
