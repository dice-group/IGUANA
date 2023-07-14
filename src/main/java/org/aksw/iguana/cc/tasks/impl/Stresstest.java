package org.aksw.iguana.cc.tasks.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
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
    ) implements Task.Config {
    }


    private final long stresstestId;
    private final Config config;

    public record PhaseExecutionConfig(
            String name,
            List<HttpWorker.Config> workers
    ) {
    }

    public record Result(
            long stresstestId,
            List<HttpWorker.Result> warmup,
            List<HttpWorker.Result> main
    ) implements Task.Config {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Stresstest.class);

    private final List<HttpWorker> warmupWorkers = new ArrayList<>();
    protected List<HttpWorker> workers = new LinkedList<>();


    public Stresstest(long stresstestId, Config config, ResponseBodyProcessorInstances responseBodyProcessorInstances) {
        this.stresstestId = stresstestId;
        this.config = config;
        long workerId = 0;
        if (config.warmupWorkers() != null)
            for (HttpWorker.Config workerConfig : config.warmupWorkers()) {
                for (int i = 0; i < workerConfig.number(); i++) {
                    var responseBodyProcessor = (workerConfig.parseResults()) ? responseBodyProcessorInstances.getProcessor(workerConfig.acceptHeader()) : null;
                    warmupWorkers.add(new SPARQLProtocolWorker(workerId++, responseBodyProcessor, (SPARQLProtocolWorker.Config) workerConfig));
                }
            }

        for (HttpWorker.Config workerConfig : config.workers()) {
            for (int i = 0; i < workerConfig.number(); i++) {
                var responseBodyProcessor = (workerConfig.parseResults()) ? responseBodyProcessorInstances.getProcessor(workerConfig.acceptHeader()) : null;
                workers.add(new SPARQLProtocolWorker(workerId++, responseBodyProcessor, (SPARQLProtocolWorker.Config) workerConfig));
            }
        }
    }

    public Result run() {
        try {

            var warmupResults = executeWorkers(warmupWorkers);
            var results = executeWorkers(workers);

            return new Result(stresstestId, warmupResults, results);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private List<HttpWorker.Result> executeWorkers(List<HttpWorker> workers) throws InterruptedException, ExecutionException {
        List<HttpWorker.Result> results = new ArrayList<>(workers.size());
        var futures = warmupWorkers.stream().map(HttpWorker::start).toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
        for (CompletableFuture<HttpWorker.Result> future : futures)
            results.add(future.get());
        return results;
    }
}
