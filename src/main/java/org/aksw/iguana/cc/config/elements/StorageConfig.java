package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Storage Configuration class
 */

public interface StorageConfig {
    @JsonProperty(index = 0, access = JsonProperty.Access.READ_ONLY)
    String type();
}



