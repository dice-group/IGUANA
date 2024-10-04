package org.aksw.iguana.cc.tasks.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.storage.Storage;
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

    public record Result(
            List<HttpWorker.Result> workerResults,
            Calendar startTime,
            Calendar endTime
    ) {}


    private static final Logger LOGGER = LoggerFactory.getLogger(Stresstest.class);

    private final List<HttpWorker> warmupWorkers = new ArrayList<>();
    private final List<HttpWorker> workers = new ArrayList<>();

    private final StresstestResultProcessor srp;


    public Stresstest(String suiteID, long stresstestID, Config config, ResponseBodyProcessorInstances responseBodyProcessorInstances, List<Storage> storages, List<Metric> metrics) {

        // initialize workers
        if (config.warmupWorkers() != null) {
            // initialize query handlers
            // count the number of workers for each query handler
            final var queryHandlers = config.warmupWorkers.stream().map(HttpWorker.Config::queries).distinct().toList();
            queryHandlers.stream().map(qh1 ->
                            List.of(qh1, config.warmupWorkers.stream()
                                    .map(HttpWorker.Config::queries)
                                    .filter(qh1::equals)
                                    .count()))
                    .forEach(list -> ((QueryHandler) list.get(0)).setTotalWorkerCount((int) (long) list.get(1)));
            long workerId = 0;
            for (HttpWorker.Config workerConfig : config.warmupWorkers()) {
                for (int i = 0; i < workerConfig.number(); i++) {
                    var responseBodyProcessor = (workerConfig.parseResults()) ? responseBodyProcessorInstances.getProcessor(workerConfig.acceptHeader()) : null;
                    warmupWorkers.add(new SPARQLProtocolWorker(workerId++, responseBodyProcessor, (SPARQLProtocolWorker.Config) workerConfig));
                }
            }
        }

        for (HttpWorker.Config workerConfig : config.workers()) {
            // initialize query handlers
            // count the number of workers for each query handler
            final var queryHandlers = config.workers.stream().map(HttpWorker.Config::queries).distinct().toList();
            queryHandlers.stream().map(qh1 ->
                            List.of(qh1, config.workers.stream()
                                    .filter(w -> w.queries().equals(qh1))
                                    .mapToInt(HttpWorker.Config::number)
                                    .sum()))
                    .forEach(list -> ((QueryHandler) list.get(0)).setTotalWorkerCount((int) list.get(1)));
            long workerId = 0;
            for (int i = 0; i < workerConfig.number(); i++) {
                var responseBodyProcessor = (workerConfig.parseResults()) ? responseBodyProcessorInstances.getProcessor(workerConfig.acceptHeader()) : null;
                workers.add(new SPARQLProtocolWorker(workerId++, responseBodyProcessor, (SPARQLProtocolWorker.Config) workerConfig));
            }
        }

        // retrieve all query ids
        Set<String> queryIDs = new HashSet<>();
        for (HttpWorker.Config wConfig : config.workers) {
            if (wConfig instanceof SPARQLProtocolWorker.Config) {
                queryIDs.addAll(List.of((wConfig).queries().getAllQueryIds()));
            }
        }

        srp = new StresstestResultProcessor(
                suiteID,
                stresstestID,
                this.workers,
                new ArrayList<>(queryIDs),
                metrics,
                storages,
                responseBodyProcessorInstances.getResults()
        );
    }

    public void run() {
        if (!warmupWorkers.isEmpty()) {
            SPARQLProtocolWorker.initHttpClient(warmupWorkers.size());
            executeWorkers(warmupWorkers); // warmup results will be dismissed
            SPARQLProtocolWorker.closeHttpClient();
        }
        SPARQLProtocolWorker.initHttpClient(workers.size());
        var results = executeWorkers(workers);
        SPARQLProtocolWorker.closeHttpClient();

        srp.process(results.workerResults);
        srp.calculateAndSaveMetrics(results.startTime, results.endTime);
    }

    private Result executeWorkers(List<HttpWorker> workers) {
        List<HttpWorker.Result> results = new ArrayList<>(workers.size());
        Calendar startTime = Calendar.getInstance(); // TODO: Calendar is outdated
        var futures = workers.stream().map(HttpWorker::start).toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        Calendar endTime = Calendar.getInstance();
        for (CompletableFuture<HttpWorker.Result> future : futures) {
            try {
                results.add(future.get());
            } catch (ExecutionException e) {
                LOGGER.error("Unexpected error during execution of worker.", e);
            } catch (InterruptedException ignored) {}

        }
        return new Result(results, startTime, endTime);
    }

    @Override
    public String getTaskName() {
        return "stresstest";
    }
}
