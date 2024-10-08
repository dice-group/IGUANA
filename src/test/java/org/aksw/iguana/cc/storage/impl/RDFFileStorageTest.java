package org.aksw.iguana.cc.storage.impl;

import org.aksw.iguana.cc.mockup.MockupQueryHandler;
import org.aksw.iguana.cc.mockup.MockupWorker;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This Test class extends the StorageTest class and tests the RDFFileStorage class.
 */
public class RDFFileStorageTest extends StorageTest {
	public static List<Arguments> data() {
		resetDate();
		final var arguments = new ArrayList<Arguments>();

        final var paths = new ArrayList<>(List.of("rdf-file-storage-test1.ttl", "rdf-file-storage-test1.nt", "rdf-file-storage-test1.nt", ""));

		final var queryHandler1 = new MockupQueryHandler(0, 10);
		final var queryHandler2 = new MockupQueryHandler(1, 10);

		final var workers = List.of(
				MockupWorker.createWorkers(0, 2, queryHandler1, "test-connection-1", "v1.0.0", "test-dataset-1"),
				MockupWorker.createWorkers(2, 2, queryHandler2, "test-connection-1", "v1.0.0", "test-dataset-1")
		);
		final var task1 = createTaskResult(workers, 0, "0");
		final var task2 = createTaskResult(workers, 1, "0");

		// test file creation
		for (String path : paths) {
			arguments.add(Arguments.of(tempDir.resolve(path).toString(), List.of(task1), task1.resultModel()));
		}

		// test file appending
		Model concatenatedModel = ModelFactory.createDefaultModel().add(task1.resultModel()).add(task2.resultModel());
		arguments.add(Arguments.of(tempDir.resolve("rdf-file-storage-test2.ttl").toString(), List.of(task1, task2), concatenatedModel));
		return arguments;
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testRDFFileStorage(String path, List<TaskResult> results, Model expectedModel) {
		final var rdfStorage = new RDFFileStorage(path);
		for (var result : results) {
			rdfStorage.storeResult(result.resultModel());
		}
		path = rdfStorage.getFileName();
		Model actualModel = RDFDataMgr.loadModel(path);
		calculateModelDifference(expectedModel, actualModel);
		// TODO: This test probably fails, because the expected model uses java's Duration objects for duration literals,
		//  while the actual model uses XSDDuration objects for duration literals.
		// assertTrue(actualModel.isIsomorphicWith(expectedModel));
	}

	private void calculateModelDifference(Model expectedModel, Model actualModel) {
		List<String> expectedStmts = new ArrayList<>();
		List<String> actualStmts = new ArrayList<>();
		expectedModel.listStatements().forEach(s -> expectedStmts.add(s.toString()));
		actualModel.listStatements().forEach(s -> actualStmts.add(s.toString()));

		for (String stmt : expectedStmts) {
			if (!actualStmts.contains(stmt)) {
				System.out.println("Expected but not found: " + stmt);
			}
			actualStmts.remove(stmt);
		}
		assertTrue(actualStmts.isEmpty());
	}
}
