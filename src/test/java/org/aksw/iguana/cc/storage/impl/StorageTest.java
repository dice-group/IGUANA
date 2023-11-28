package org.aksw.iguana.cc.storage.impl;

import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.metrics.impl.*;
import org.aksw.iguana.cc.mockup.MockupWorker;
import org.aksw.iguana.cc.storage.Storable;
import org.aksw.iguana.cc.storage.Storage;
import org.aksw.iguana.cc.mockup.MockupStorage;
import org.aksw.iguana.cc.tasks.impl.StresstestResultProcessor;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    public static class TestStorable implements Storable.AsCSV, Storable.AsRDF {

        @Override
        public Storable.CSVData toCSV() {
            final var data = new Storable.CSVData("csv-folder", List.of(
                    new Storable.CSVData.CSVFileData("csv-file-1", new String[][]{{"header-1", "header-2"}, {"randomString", "100"}}),
                    new Storable.CSVData.CSVFileData("csv-file-2", new String[][]{{"header-1", "header-2"}, {"randomString-2", "200"}, {"randomString-3", "300"}})
            ));
            return data;
        }

        @Override
        public Model toRDF() {
            Model m = ModelFactory.createDefaultModel();
            m.add(m.createResource("http://example.org/subject"), m.createProperty("http://example.org/predicate"), m.createResource("http://example.org/object"));
            return m;
        }
    }

    @BeforeEach
    public void resetDate() {
        someDateTime = GregorianCalendar.from(ZonedDateTime.ofInstant(Instant.parse("2023-10-21T20:48:06.399Z"), ZoneId.of("Europe/Berlin")));
    }

    public record TaskResult(Model resultModel, List<HttpWorker.Result> workerResults) {}

    protected static Path tempDir;

    private static Calendar someDateTime = GregorianCalendar.from(ZonedDateTime.ofInstant(Instant.parse("2023-10-21T20:48:06.399Z"), ZoneId.of("Europe/Berlin")));

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
    protected static TaskResult createTaskResult(List<List<HttpWorker>> workers, int taskID, String suiteID) {
        final var queryIDs = new ArrayList<String>();
        for (var list : workers) {
            queryIDs.addAll(List.of(list.get(0).config().queries().getAllQueryIds()));
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
