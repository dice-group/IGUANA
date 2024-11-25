package org.dice_research.iguana.cc.suite;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dice_research.iguana.cc.config.elements.ConnectionConfig;
import org.dice_research.iguana.cc.config.elements.DatasetConfig;
import org.dice_research.iguana.cc.config.elements.StorageConfig;
import org.dice_research.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.metrics.impl.*;
import org.dice_research.iguana.cc.storage.Storage;
import org.dice_research.iguana.cc.storage.impl.CSVStorage;
import org.dice_research.iguana.cc.storage.impl.RDFFileStorage;
import org.dice_research.iguana.cc.storage.impl.TriplestoreStorage;
import org.dice_research.iguana.cc.tasks.impl.Stresstest;
import org.dice_research.iguana.cc.tasks.Task;
import org.dice_research.iguana.cc.worker.ResponseBodyProcessor;
import org.dice_research.iguana.cc.worker.ResponseBodyProcessorInstances;
import org.dice_research.iguana.cc.metrics.impl.*;
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
            List<Metric> metrics,
            @JsonProperty List<ResponseBodyProcessor.Config> responseBodyProcessors
    ) {}


    private final String suiteId;
    private final Config config;
    private final ResponseBodyProcessorInstances responseBodyProcessorInstances;

    private final static Logger LOGGER = LoggerFactory.getLogger(Suite.class);

    private final List<Task> tasks = new ArrayList<>();

    Suite(String suiteId, Config config) {
        this.suiteId = suiteId;
        this.config = config;
        long taskID = 0;

        responseBodyProcessorInstances = new ResponseBodyProcessorInstances(config.responseBodyProcessors);
        List<Metric> metrics = initialiseMetrics(this.config.metrics);
        List<Storage> storages = initialiseStorages(this.config.storages, metrics, this.suiteId);

        for (Task.Config task : config.tasks()) {
            if (task instanceof Stresstest.Config) {
                tasks.add(new Stresstest(this.suiteId, taskID++, (Stresstest.Config) task, responseBodyProcessorInstances, storages, metrics));
            }
        }
    }

    private static List<Metric> initialiseMetrics(List<Metric> metrics) {
        if (metrics != null && !metrics.isEmpty()) {
            return metrics;
        }

        final List<Metric> out = new ArrayList<>();
        out.add(new QPS());
        out.add(new AvgQPS());
        out.add(new NoQPH());
        out.add(new AggregatedExecutionStatistics());
        out.add(new EachExecutionStatistic());
        out.add(new NoQ());
        out.add(new QMPH());
        return out;
    }

    private static List<Storage> initialiseStorages(List<StorageConfig> configs, List<Metric> metrics, String suiteID) {
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
            LOGGER.info("Task/{} {}  starting.", tasks.get(i).getTaskName(), i);
            tasks.get(i).run();
            LOGGER.info("Task/{} {}  finished.", tasks.get(i).getTaskName(), i);
        }
    }
}


