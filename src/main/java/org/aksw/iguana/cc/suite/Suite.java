package org.aksw.iguana.cc.suite;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.config.elements.DatasetConfig;
import org.aksw.iguana.cc.config.elements.StorageConfig;
import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.metrics.impl.*;
import org.aksw.iguana.cc.storage.Storage;
import org.aksw.iguana.cc.storage.impl.CSVStorage;
import org.aksw.iguana.cc.storage.impl.RDFFileStorage;
import org.aksw.iguana.cc.storage.impl.TriplestoreStorage;
import org.aksw.iguana.cc.tasks.impl.Stresstest;
import org.aksw.iguana.cc.tasks.Task;
import org.aksw.iguana.cc.worker.ResponseBodyProcessor;
import org.aksw.iguana.cc.worker.ResponseBodyProcessorInstances;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            List<StorageConfig> storages,
            // @JsonProperty List<Metric> metrics
            @JsonProperty List<ResponseBodyProcessor.Config> responseBodyProcessors
    ) {}


    private final long suiteId;
    private final Config config;
    private final ResponseBodyProcessorInstances responseBodyProcessorInstances;

    private final static Logger LOGGER = LoggerFactory.getLogger(Suite.class);

    private final List<Task> tasks = new ArrayList<>();

    Suite(long suiteId, Config config) {
        this.suiteId = suiteId;
        this.config = config;
        long taskID = 0;

        responseBodyProcessorInstances = new ResponseBodyProcessorInstances(config.responseBodyProcessors);
        List<Metric> metrics = initialiseMetrics();
        List<Storage> storages = initialiseStorages(this.config.storages, metrics, this.suiteId);

        for (Task.Config task : config.tasks()) {
            if (task instanceof Stresstest.Config) {
                tasks.add(new Stresstest(this.suiteId, taskID++, (Stresstest.Config) task, responseBodyProcessorInstances, storages, metrics));
            }
        }
    }

    private static List<Metric> initialiseMetrics() {
        final List<Metric> out = new ArrayList<>();
        // TODO: make this configurable
        out.add(new QPS());
        out.add(new AvgQPS());
        out.add(new NoQPH());
        out.add(new AggregatedExecutionStatistics());
        out.add(new EachExecutionStatistic());
        return out;
    }

    private static List<Storage> initialiseStorages(List<StorageConfig> configs, List<Metric> metrics, long suiteID) {
        List<Storage> out = new ArrayList<>();
        for (var storageConfig : configs) {
            if (storageConfig instanceof CSVStorage.Config) {
                out.add(new CSVStorage((CSVStorage.Config) storageConfig, metrics, suiteID));
            }
            else if (storageConfig instanceof TriplestoreStorage.Config) {
                out.add(new TriplestoreStorage((TriplestoreStorage.Config) storageConfig));
            }
            else if (storageConfig instanceof RDFFileStorage.Config) {
                out.add(new RDFFileStorage((RDFFileStorage.Config) storageConfig));
            }
        }
        return out;
    }

    public void run() {
        for (int i = 0; i < tasks.size(); i++) {
            tasks.get(i).run();
            LOGGER.info("Task/{} {}  finished.", tasks.get(i).getTaskName(), i);
        }
    }
}


