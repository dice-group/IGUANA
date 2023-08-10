package org.aksw.iguana.cc.tasks.storage.impl;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.metrics.MetricManager;
import org.aksw.iguana.cc.metrics.impl.AggregatedExecutionStatistics;
import org.aksw.iguana.cc.metrics.impl.NoQ;
import org.aksw.iguana.cc.metrics.impl.NoQPH;
import org.aksw.iguana.cc.metrics.impl.QPS;
import org.aksw.iguana.cc.storage.impl.CSVStorage;
import org.aksw.iguana.commons.rdf.IONT;
import org.aksw.iguana.commons.rdf.IPROP;
import org.aksw.iguana.commons.rdf.IRES;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class CSVStorageTest {
    CSVStorage storage;
    Path folder;
    Path suiteFolder;

    @BeforeEach
    public void setup() throws IOException {
        this.folder = Files.createTempDirectory("iguana-CSVStorage-test");

        // TODO: suiteid
        this.suiteFolder = folder.resolve("suite");
    }

    public static Arguments createTestData1() {
        // Entry records should store metric values in the same order as the metrics in the following list.
        List<Metric> metrics = List.of(new NoQ(), new NoQPH(), new QPS(), new AggregatedExecutionStatistics());

        // First Task has 2 Worker
        Resource datasetRes = IRES.getResource("dataset1");
        Resource experimentRes = IRES.getResource("suite/experiment");
        Resource taskRes = IRES.getResource("suite/experiment/task1");
        Resource conRes = IRES.getResource("triplestore1");
        Resource workerRes1 = IRES.getResource("worker1");
        Resource workerRes2 = IRES.getResource("worker2");
        Resource taskQueryRes = IRES.getResource("task-query");
        Resource workerQueryRes1 = IRES.getResource("worker-query-1");
        Resource workerQueryRes2 = IRES.getResource("worker-query-2");
        Resource queryRes1 = IRES.getResource("query1");
        Resource queryRes2 = IRES.getResource("query2");

        Model m = ModelFactory.createDefaultModel();

        m.add(experimentRes, RDF.type, IONT.experiment);
        m.add(experimentRes, IPROP.dataset, datasetRes);
        m.add(experimentRes, IPROP.task, taskRes);
        m.add(datasetRes, RDFS.label, ResourceFactory.createTypedLiteral("dataset1"));
        m.add(datasetRes, RDF.type, IONT.dataset);
        m.add(conRes, RDF.type, IONT.connection);
        m.add(conRes, RDFS.label, ResourceFactory.createTypedLiteral("triplestore1"));
        m.add(conRes, IPROP.version, ResourceFactory.createTypedLiteral("v1"));
        m.add(taskRes, RDF.type, IONT.task);
        m.add(taskRes, IPROP.connection, conRes);
        m.add(taskRes, IPROP.startDate, ResourceFactory.createTypedLiteral("now"));
        m.add(taskRes, IPROP.endDate, ResourceFactory.createTypedLiteral("then"));
        m.add(taskRes, IPROP.workerResult, workerRes1);
        m.add(taskRes, IPROP.workerResult, workerRes2);
        m.add(taskRes, IPROP.noOfWorkers, ResourceFactory.createTypedLiteral(2));
        m.add(taskRes, IPROP.createMetricProperty(new NoQ()), ResourceFactory.createTypedLiteral(BigInteger.valueOf(2)));
        m.add(taskRes, IPROP.createMetricProperty(new NoQPH()), ResourceFactory.createTypedLiteral(BigDecimal.valueOf(20,2)));
        m.add(taskRes, IPROP.query, taskQueryRes);

        m.add(workerRes1, RDF.type, IONT.worker);
        m.add(workerRes1, IPROP.workerID, ResourceFactory.createTypedLiteral(BigInteger.valueOf(1)));
        m.add(workerRes1, IPROP.workerType, ResourceFactory.createTypedLiteral("SPARQL"));
        m.add(workerRes1, IPROP.noOfQueries, ResourceFactory.createTypedLiteral(BigInteger.valueOf(2)));
        m.add(workerRes1, IPROP.timeOut, ResourceFactory.createTypedLiteral(BigInteger.valueOf(100)));
        m.add(workerRes1, IPROP.createMetricProperty(new NoQ()), ResourceFactory.createTypedLiteral(BigInteger.valueOf(8)));
        m.add(workerRes1, IPROP.createMetricProperty(new NoQPH()), ResourceFactory.createTypedLiteral(BigDecimal.valueOf(108)));
        m.add(workerRes1, IPROP.query, workerQueryRes1);

        m.add(workerRes2, RDF.type, IONT.worker);
        m.add(workerRes2, IPROP.workerID, ResourceFactory.createTypedLiteral(BigInteger.valueOf(2)));
        m.add(workerRes2, IPROP.workerType, ResourceFactory.createTypedLiteral("LQRAPS"));
        m.add(workerRes2, IPROP.noOfQueries, ResourceFactory.createTypedLiteral(BigInteger.valueOf(1)));
        m.add(workerRes2, IPROP.timeOut, ResourceFactory.createTypedLiteral(BigInteger.valueOf(50)));
        m.add(workerRes2, IPROP.createMetricProperty(new NoQ()), ResourceFactory.createTypedLiteral(BigDecimal.valueOf(0)));
        m.add(workerRes2, IPROP.createMetricProperty(new NoQPH()), ResourceFactory.createTypedLiteral(BigDecimal.valueOf(0)));
        m.add(workerRes2, IPROP.query, workerQueryRes2);

        m.add(taskQueryRes, IPROP.createMetricProperty(new QPS()), ResourceFactory.createTypedLiteral(BigDecimal.valueOf(72000, 2)));
        m.add(taskQueryRes, IPROP.succeeded, ResourceFactory.createTypedLiteral(BigInteger.valueOf(2)));
        m.add(taskQueryRes, IPROP.failed, ResourceFactory.createTypedLiteral(BigInteger.valueOf(0)));
        m.add(taskQueryRes, IPROP.totalTime, ResourceFactory.createTypedLiteral(BigInteger.valueOf(12345)));
        m.add(taskQueryRes, IPROP.resultSize, ResourceFactory.createTypedLiteral(BigInteger.valueOf(1000)));
        m.add(taskQueryRes, IPROP.wrongCodes, ResourceFactory.createTypedLiteral(BigInteger.valueOf(0)));
        m.add(taskQueryRes, IPROP.timeOuts, ResourceFactory.createTypedLiteral(BigInteger.valueOf(0)));
        m.add(taskQueryRes, IPROP.unknownException, ResourceFactory.createTypedLiteral(BigInteger.valueOf(0)));
        m.add(taskQueryRes, IPROP.queryID, queryRes1);
        m.add(taskQueryRes, IPROP.createMetricProperty(new QPS()), ResourceFactory.createTypedLiteral(BigDecimal.valueOf(72000, 2)));

        m.add(workerQueryRes1, IPROP.createMetricProperty(new QPS()), ResourceFactory.createTypedLiteral(BigDecimal.valueOf(10)));
        m.add(workerQueryRes1, IPROP.succeeded, ResourceFactory.createTypedLiteral(BigInteger.valueOf(1)));
        m.add(workerQueryRes1, IPROP.failed, ResourceFactory.createTypedLiteral(BigInteger.valueOf(2)));
        m.add(workerQueryRes1, IPROP.totalTime, ResourceFactory.createTypedLiteral(BigInteger.valueOf(100)));
        m.add(workerQueryRes1, IPROP.resultSize, ResourceFactory.createTypedLiteral(BigInteger.valueOf(98)));
        m.add(workerQueryRes1, IPROP.wrongCodes, ResourceFactory.createTypedLiteral(BigInteger.valueOf(3)));
        m.add(workerQueryRes1, IPROP.timeOuts, ResourceFactory.createTypedLiteral(BigInteger.valueOf(4)));
        m.add(workerQueryRes1, IPROP.unknownException, ResourceFactory.createTypedLiteral(BigInteger.valueOf(5)));
        m.add(workerQueryRes1, IPROP.queryID, queryRes1);

        // workerQueryRes2 isn't complete, therefore won't be saved
        m.add(workerQueryRes2, IPROP.queryID, queryRes2);

        Path testFileFolder = Path.of("src/test/resources/storage/csv_test_files/");

        return Arguments.of(Named.of(String.format("One simple tasks with one faulty entry. | ExpectedFolder: %s | Metrics: %s", testFileFolder, metrics.stream().map(Metric::getAbbreviation).toList()), new Suite(List.of(m), metrics, testFileFolder)));
    }

    @AfterEach
    public void cleanup() throws IOException {
        FileUtils.deleteDirectory(this.folder.toFile());
    }

    public static Stream<Arguments> data() {
        return Stream.of(
            createTestData1()
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @DisplayName("Test CSVStorage")
    public void testCSVStorage(Suite suite) throws IOException {
        for (Model m : suite.taskResults) {
            // Metrics need to be set here, because the CSVStorage uses the manager to store the results
            MetricManager.setMetrics(suite.metrics);

            // Test Initialisation
            assertDoesNotThrow(() -> storage = new CSVStorage(this.folder.toAbsolutePath().toString()), "Initialisation failed");
            assertTrue(Files.exists(this.suiteFolder), String.format("Result folder (%s) doesn't exist", this.suiteFolder));
            storage.storeResult(m);

            List<Path> expectedFiles;
            try (Stream<Path> s = Files.list(suite.expectedFolder)) {
                expectedFiles = s.toList();
            }

            for (Path expectedFile : expectedFiles) {
                Path actualFile = suiteFolder.resolve(expectedFile.getFileName());
                assertTrue(Files.exists(actualFile), String.format("File (%s) doesn't exist", actualFile));
                assertDoesNotThrow(() -> compareCSVFiles(expectedFile, actualFile));
            }
        }
    }

    private void compareCSVFiles(Path expected, Path actual) throws IOException, CsvException {
        try (CSVReader readerExpected = new CSVReader(new FileReader(expected.toFile()));
             CSVReader readerActual = new CSVReader(new FileReader(actual.toFile()))) {
            String[] headerExpected = readerExpected.readNext();
            String[] headerActual = readerActual.readNext();
            assertEquals(headerExpected.length, headerActual.length, String.format("Headers don't match. Actual: %s, Expected: %s", Arrays.toString(headerActual), Arrays.toString(headerExpected)));
            for (int i = 0; i < headerExpected.length; i++) {
                assertEquals(headerExpected[i], headerActual[i], String.format("Headers don't match. Actual: %s, Expected: %s", Arrays.toString(headerActual), Arrays.toString(headerExpected)));
            }

            List<String[]> expectedValues = new ArrayList<>(readerExpected.readAll());
            List<String[]> actualValues = new ArrayList<>(readerActual.readAll());

            for (String[] expectedLine : expectedValues) {
                List<String[]> sameLines = actualValues.stream().filter(x -> {
                    for (int i = 0; i < expectedLine.length; i++) {
                        if (!expectedLine[i].equals(x[i])) return false;
                    }
                    return true;
                }).toList();

                assertFalse(sameLines.isEmpty(), String.format("Line (%s) not found in actual file", Arrays.toString(expectedLine)));
                actualValues.remove(sameLines.get(0));
            }
            assertTrue(actualValues.isEmpty(), String.format("Actual file contains more lines than expected. Lines: %s", actualValues.stream().map(x -> "[" + String.join(", ", x) + "]").collect(Collectors.joining("\n"))));
        }
    }

    private record Suite(List<Model> taskResults, List<Metric> metrics, Path expectedFolder) {}
}
