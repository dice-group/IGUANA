package org.aksw.iguana.cc.storage.impl;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.aksw.iguana.cc.mockup.MockupQueryHandler;
import org.aksw.iguana.cc.mockup.MockupWorker;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class CSVStorageTest extends StorageTest {
    private static final String EXPECTED_FILES_DIR = "src/test/resources/test-data/csv-storage-test/";

    public static List<Arguments> data() {
        resetDate();
        final var workersTask1 = List.of(
                MockupWorker.createWorkers(0, 2, new MockupQueryHandler(0, 10), "test-connection-1", "v1.0.0", "test-dataset-1"),
                MockupWorker.createWorkers(2, 2, new MockupQueryHandler(1, 10), "test-connection-2", "v1.1.0", "test-dataset-2")
        );

        final var workersTask2 = List.of(
                MockupWorker.createWorkers(0, 2, new MockupQueryHandler(2, 5), "test-connection-3", "v1.2.0", "test-dataset-3"),
                MockupWorker.createWorkers(2, 2, new MockupQueryHandler(3, 5), "test-connection-4", "v1.3.0", "test-dataset-4")
        );

        return List.of(Arguments.of(List.of(createTaskResult(workersTask1, 0, "123"), createTaskResult(workersTask2, 1, "123"))));
    }

    @ParameterizedTest
    @MethodSource("data")
    protected void testCSVStorage(List<TaskResult> results) throws IOException {
        final var storage = new CSVStorage(tempDir.toString(), getMetrics(), "123");
        for (var result : results)
            storage.storeResult(result.resultModel());

        final var expectedFiles = Path.of(EXPECTED_FILES_DIR);
        final var actualFiles = tempDir;

        try (var files = Files.list(expectedFiles)) {
            files.forEach(
                    x -> {
                        try {
                            compareFile(x, actualFiles.resolve(x.getFileName()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
        }

        storage.storeData(new TestStorable());
        final var path = tempDir.resolve("suite-123").resolve("task-1").resolve("csv-folder").toFile();
        assertTrue(path.exists());
        assertTrue(path.isDirectory());
        assertEquals(2, path.listFiles().length);
        for (var file : path.listFiles()) {
            if (file.getName().equals("csv-file-1.csv"))
                assertEquals(2, Files.readAllLines(file.toPath()).size());
            else if (file.getName().equals("csv-file-2.csv"))
                assertEquals(3, Files.readAllLines(file.toPath()).size());
            else
                throw new RuntimeException("Unexpected file name: " + file.getName());
        }
    }

    private void compareFile(Path expected, Path actual) throws IOException {
        if (Files.isDirectory(expected)) {
            assertTrue(Files.isDirectory(actual), String.format("Expected directory %s but found file %s", expected, actual));
            assertEquals(expected.getFileName(), actual.getFileName());
            try (var files = Files.list(expected)) {
                files.forEach(x -> {
                    try {
                        compareFile(x, actual.resolve(x.getFileName()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } else if (Files.isRegularFile(expected)) {
            assertTrue(Files.isRegularFile(actual), String.format("Expected file %s but found directory %s", expected, actual));
            assertEquals(expected.getFileName(), actual.getFileName());
            compareCSVFiles(expected, actual);
        } else {
            throw new RuntimeException(String.format("Expected file or directory %s but found nothing", expected));
        }
    }

    private void compareCSVFiles(Path expected, Path actual) throws IOException {
        try (CSVReader readerExpected = new CSVReader(new FileReader(expected.toFile()));
             CSVReader readerActual = new CSVReader(new FileReader(actual.toFile()))) {

            String[] headerExpected;
            String[] headerActual;
            try {
                headerExpected = readerExpected.readNext();
                headerActual = readerActual.readNext();
            } catch (CsvException e) {
                throw new RuntimeException(String.format("CSV format in the header of file %s is malformed.", expected), e);
            }

            assertEquals(headerExpected.length, headerActual.length, String.format("Headers don't match. Actual: %s, Expected: %s", Arrays.toString(headerActual), Arrays.toString(headerExpected)));
            for (int i = 0; i < headerExpected.length; i++) {
                assertEquals(headerExpected[i], headerActual[i], String.format("Headers don't match. Actual: %s, Expected: %s", Arrays.toString(headerActual), Arrays.toString(headerExpected)));
            }

            List<String[]> expectedValues;
            List<String[]> actualValues;

            try {
                expectedValues = new ArrayList<>(readerExpected.readAll());
                actualValues = new ArrayList<>(readerActual.readAll());
            } catch (CsvException e) {
                throw new RuntimeException(String.format("CSV format in file %s is malformed.", expected), e);
            }

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
}
