package org.aksw.iguana.cc.suite;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.config.elements.DatasetConfig;
import org.aksw.iguana.cc.config.elements.StorageConfig;
import org.aksw.iguana.cc.tasks.impl.Stresstest;
import org.aksw.iguana.cc.tasks.Task;
import org.aksw.iguana.cc.worker.ResponseBodyProcessorInstances;

import java.util.ArrayList;
import java.util.List;

public class Suite {

    public record Config(
            @JsonProperty(required = true)
            List<DatasetConfig> datasets,
            @JsonProperty(required = true)
            List<ConnectionConfig> connections,
            @JsonProperty(required = true)
            List<Task.Config> tasks,
            @JsonProperty
            String preScriptHook,
            @JsonProperty
            String postScriptHook,
            @JsonProperty
            List<StorageConfig> storages) {
    }

    public record Result(List<Stresstest.Result> stresstest) {

    }

    private final long suiteId;
    private final Config config;
    private final ResponseBodyProcessorInstances responseBodyProcessorInstances;

    private final List<Stresstest> stresstests = new ArrayList<>();

    Suite(long suiteId, Config config){

        this.suiteId = suiteId;
        this.config = config;
        long stresstestId = 0;
        responseBodyProcessorInstances = new ResponseBodyProcessorInstances();


        for (Task.Config task : config.tasks()) {
            if (task instanceof Stresstest.Config) {
                stresstests.add(new Stresstest(stresstestId, (Stresstest.Config) task, responseBodyProcessorInstances));
            }
        }
    }

    public Suite.Result run() {
        List<Stresstest.Result> stresstestResults = new ArrayList<>();
        for (Stresstest stresstest : stresstests) {
            stresstestResults.add(stresstest.run());
        }
        return new Result(stresstestResults);
    }

}


