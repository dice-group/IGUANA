package org.aksw.iguana.cc.storage.impl;

import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.metrics.impl.*;
import org.aksw.iguana.cc.mockup.MockupWorker;
import org.aksw.iguana.cc.storage.Storage;
import org.aksw.iguana.cc.mockup.MockupStorage;
import org.aksw.iguana.cc.tasks.impl.StresstestResultProcessor;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;


public abstract class StorageTest {
    @BeforeAll
    public static void createFolder() throws IOException {
        tempDir = Files.createTempDirectory("iguana-storage-test-dir");
    }

    @AfterAll
    public static void deleteFolder() throws IOException {
        FileUtils.deleteDirectory(tempDir.toFile());
    }
    protected record TaskResult(Model resultModel, List<HttpWorker.Result> workerResults) {}

    protected static final long suiteID = 0;
    protected static Path tempDir;

    private static final Calendar someDateTime = new Calendar.Builder()
            .setDate(2023, 10, 21)
            .setTimeOfDay(20, 48, 6, 399)
            .setLocale(Locale.GERMANY)
            .build();

    private static Calendar getDateTime() {
        someDateTime.add(Calendar.MINUTE, 1);
        someDateTime.add(Calendar.SECOND, 18);
        return someDateTime;
    }

    public static List<Metric> getMetrics() {
        return List.of(
                new AggregatedExecutionStatistics(),
                new AvgQPS(),
                new EachExecutionStatistic(),
                new NoQ(),
                new NoQPH(),
                new PAvgQPS(1000),
                new PQPS(1000),
                new QMPH(),
                new QPS()
        );
    }

    // Argument is a List that contains lists of workers with the same configuration.
    protected static TaskResult createTaskResult(List<List<HttpWorker>> workers, int taskID) {
        final var queryIDs = new ArrayList<String>();
        for (var list : workers) {
            queryIDs.addAll(List.of(list.get(0).config().queries().getAllQueryIds())); // I'm not going to check for empty lists
        }

        final var metrics = getMetrics();
        final var storages = new ArrayList<Storage>();
        final Supplier<Map<LanguageProcessor, List<LanguageProcessor.LanguageProcessingData>>> supplier = HashMap::new;

        final var ls = new MockupStorage();
        storages.add(ls);

        final var flatWorkerList = workers.stream().flatMap(Collection::stream).toList();

        final var srp = new StresstestResultProcessor(suiteID, taskID, flatWorkerList, queryIDs, metrics, storages, supplier);

        final var workerResults = new ArrayList<HttpWorker.Result>();
        for (var list : workers) {
            workerResults.addAll(MockupWorker.createWorkerResults(list.get(0).config().queries(), list));
        }

        srp.process(workerResults);
        Calendar startTime = (Calendar) getDateTime().clone();
        srp.calculateAndSaveMetrics(startTime, getDateTime());

        return new TaskResult(ls.getResultModel(), workerResults);
    }

}
