package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Dataset config class.
 * <p>
 * Will set the name and if it was set in the config file the fileName
 */
public record DatasetConfig(
        @JsonProperty(required = true) String name,
        @JsonProperty String file) {
}
