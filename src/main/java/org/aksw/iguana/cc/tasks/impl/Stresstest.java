package org.aksw.iguana.cc.tasks.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.cc.config.elements.StorageConfig;
import org.aksw.iguana.cc.metrics.MetricManager;
import org.aksw.iguana.cc.metrics.impl.AggregatedExecutionStatistics;
import org.aksw.iguana.cc.metrics.impl.AvgQPS;
import org.aksw.iguana.cc.metrics.impl.NoQPH;
import org.aksw.iguana.cc.metrics.impl.QPS;
import org.aksw.iguana.cc.storage.Storage;
import org.aksw.iguana.cc.storage.impl.CSVStorage;
import org.aksw.iguana.cc.storage.impl.RDFFileStorage;
import org.aksw.iguana.cc.tasks.Task;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.cc.worker.ResponseBodyProcessorInstances;
import org.aksw.iguana.cc.worker.impl.SPARQLProtocolWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;


/**
 * Stresstest.
 * Will stresstest a connection using several Workers (simulated Users) each in one thread.
 */
public class Stresstest implements Task {

    public record Config(
        List<HttpWorker.Config> warmupWorkers,
        @JsonProperty(required = true) List<HttpWorker.Config> workers
    ) implements Task.Config {}

    public record PhaseExecutionConfig(
            String name,
            List<HttpWorker.Config> workers
    ) {}

    public record Result(
            List<HttpWorker.Result> workerResults,
            Calendar startTime,
            Calendar endTime
    ) {}


    private final long stresstestId;
    private final Config config;


    private static final Logger LOGGER = LoggerFactory.getLogger(Stresstest.class);

    private final List<HttpWorker> warmupWorkers = new ArrayList<>();
    private List<HttpWorker> workers = new ArrayList<>();

    public List<Storage> storages = new ArrayList<>();


    public Stresstest(long stresstestId, Config config, ResponseBodyProcessorInstances responseBodyProcessorInstances, List<StorageConfig> storages) {
        this.stresstestId = stresstestId;
        this.config = config;
        for (var storageConfig : storages) {
            if (storageConfig instanceof CSVStorage.Config) {
                this.storages.add(new CSVStorage((CSVStorage.Config) storageConfig));
            }
            else if (storageConfig instanceof RDFFileStorage.Config) {
                this.storages.add(new RDFFileStorage((RDFFileStorage.Config) storageConfig));
            }
        }

        long workerId = 0;
        if (config.warmupWorkers() != null) {
            for (HttpWorker.Config workerConfig : config.warmupWorkers()) {
                for (int i = 0; i < workerConfig.number(); i++) {
                    var responseBodyProcessor = (workerConfig.parseResults()) ? responseBodyProcessorInstances.getProcessor(workerConfig.acceptHeader()) : null;
                    warmupWorkers.add(new SPARQLProtocolWorker(workerId++, responseBodyProcessor, (SPARQLProtocolWorker.Config) workerConfig));
                }
            }
        }

        for (HttpWorker.Config workerConfig : config.workers()) {
            for (int i = 0; i < workerConfig.number(); i++) {
                var responseBodyProcessor = (workerConfig.parseResults()) ? responseBodyProcessorInstances.getProcessor(workerConfig.acceptHeader()) : null;
                workers.add(new SPARQLProtocolWorker(workerId++, responseBodyProcessor, (SPARQLProtocolWorker.Config) workerConfig));
            }
        }
    }

    public void run() {
        try {
            var warmupResults = executeWorkers(warmupWorkers);
            var results = executeWorkers(workers);

            LOGGER.info("Stresstest {} finished", stresstestId);

            Set<String> queryIDs = new HashSet<>();
            for (HttpWorker.Config wConfig : this.config.workers) {
                if (wConfig instanceof SPARQLProtocolWorker.Config) {
                    queryIDs.addAll(List.of((wConfig).queries().getAllQueryIds()));
                }
            }

            // TODO: language processor

            // TODO: maybe add this to the configd
            MetricManager.setMetrics(List.of(new QPS(), new AvgQPS(), new NoQPH(), new AggregatedExecutionStatistics()));

            // TODO: suiteID
            StresstestResultProcessor srp = new StresstestResultProcessor(
                    0L,
                    this.stresstestId,
                    this.workers,
                    new ArrayList<>(queryIDs),
                    this.storages
            );

            srp.process(results.workerResults);
            srp.calculateAndSaveMetrics(results.startTime, results.endTime, null);


        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Result executeWorkers(List<HttpWorker> workers) throws InterruptedException, ExecutionException {
        List<HttpWorker.Result> results = new ArrayList<>(workers.size());
        Calendar startTime = Calendar.getInstance();
        var futures = workers.stream().map(HttpWorker::start).toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
        Calendar endTime = Calendar.getInstance();
        for (CompletableFuture<HttpWorker.Result> future : futures)
            results.add(future.get());
        return new Result(results, startTime, endTime);
    }
}
