package org.aksw.iguana.cc.suite;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
            @JsonIgnore
            List<DatasetConfig> datasets, /* Will already be consumed and ignored herein */
            @JsonIgnore
            List<ConnectionConfig> connections, /* Will already be consumed and ignored herein */
            @JsonProperty(required = true)
            List<Task.Config> tasks,
            List<StorageConfig> storages) {
    }


    private final long suiteId;
    private final Config config;
    private final ResponseBodyProcessorInstances responseBodyProcessorInstances;

    private final List<Task> tasks = new ArrayList<>();

    Suite(long suiteId, Config config) {
        this.suiteId = suiteId;
        this.config = config;
        long taskID = 0;
        responseBodyProcessorInstances = new ResponseBodyProcessorInstances();

        for (Task.Config task : config.tasks()) {
            if (task instanceof Stresstest.Config) {
                tasks.add(new Stresstest(taskID++, (Stresstest.Config) task, responseBodyProcessorInstances, this.config.storages)); // TODO: look for a better way to add the storages
            }
        }
    }

    public void run() {
        for (Task task : tasks) {
            task.run();
        }
    }

}


