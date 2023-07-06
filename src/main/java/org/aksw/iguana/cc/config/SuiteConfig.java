package org.aksw.iguana.cc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.cc.config.elements.*;
import org.aksw.iguana.cc.tasks.Stresstest;

import java.util.List;

public record SuiteConfig(
        @JsonProperty(required = true)
        List<DatasetConfig> datasets,
        @JsonProperty(required = true)
        List<ConnectionConfig> connections,
        @JsonProperty(required = true)
        List<TaskConfig> tasks,
        @JsonProperty
        String preScriptHook,
        @JsonProperty
        String postScriptHook,
        @JsonProperty
        List<StorageConfig> storages) {
}
