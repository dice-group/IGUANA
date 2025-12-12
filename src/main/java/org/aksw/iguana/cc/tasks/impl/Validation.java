package org.aksw.iguana.cc.tasks.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.storage.Storage;
import org.aksw.iguana.cc.tasks.Task;
import org.aksw.iguana.cc.worker.ResponseBodyProcessorInstances;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class Validation implements Task {

    public record Config(
            @JsonProperty(required = true) TriplestoreTuple groundTruth,
            @JsonProperty(required = true) List<TriplestoreTuple> validate
    ) implements Task.Config {}

    public record TriplestoreTuple(
            String triplestoreName,
            StresstestResultReference resultParsingStresstest
    ) {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Validation.CSVResultPath.class),
            @JsonSubTypes.Type(value = Validation.TaskID.class)
    })
    sealed public interface StresstestResultReference permits CSVResultPath, TaskID {}
    public record CSVResultPath(@JsonProperty(required = true) String csvResultPath) implements StresstestResultReference {}
    public record TaskID(@JsonProperty(required = true) int stresstestId) implements StresstestResultReference {}

    public record Result() {}

    private final Config config;

    public Validation(String suiteID, long taskId, Validation.Config config, ResponseBodyProcessorInstances responseBodyProcessorInstances, List<Storage> storages, List<Metric> metrics) {
        this.config = config;
    }

    @Override
    public void run() {
        var referenceTable = getTriplestoreResults(config.groundTruth.resultParsingStresstest);
        referenceTable = referenceTable.insertColumn(0, StringColumn.create("triplestore", Collections.nCopies(referenceTable.rowCount(), config.groundTruth.triplestoreName)));
        referenceTable = referenceTable.sortAscendingOn("queryID", "run");

        for (TriplestoreTuple validateTuple : config.validate) {
            var validateTable = getTriplestoreResults(validateTuple.resultParsingStresstest);
            validateTable = validateTable.insertColumn(0, StringColumn.create("triplestore", Collections.nCopies(validateTable.rowCount(), validateTuple.triplestoreName)));
            validateTable = validateTable.sortAscendingOn("queryID", "run");

            var diffTable = referenceTable.append(validateTable);
            diffTable = diffTable.selectColumns("triplestore", "queryID", "run", "results", "bindings", "variables", "links");
            diffTable = diffTable.sortAscendingOn("queryID", "run", "triplestore");
            diffTable = removeEqualPairs(diffTable, List.of("triplestore"));

            if (diffTable.rowCount() == 0) {
                System.out.println("Validation successful for triplestore: " + validateTuple.triplestoreName);
            } else {
                System.out.println("Validation failed for triplestore: " + validateTuple.triplestoreName);
                System.out.println("Differences found:");
                System.out.println(diffTable.printAll());
            }
        }

    }

    /**
     * Removes pairs of equal rows from the table.
     *
     * @param table
     * @return
     */
    private Table removeEqualPairs(Table table, List<String> ignoreColumns) {
        var newTable = table.emptyCopy();
        for (int i = 0; i < table.rowCount() - 1; i += 2) {
            final var row1 = table.row(i);
            final var row2 = table.row(i + 1);
            for (int col = 0; col < table.columnCount(); col++) {
                if (ignoreColumns.contains(table.column(col).name())) continue; // skip ignored columns
                var val1 = row1.getObject(col);
                var val2 = row2.getObject(col);

                if ((val1 == null && val2 != null) || (val1 != null && !val1.equals(val2))) {
                    // rows are different
                    newTable.append(row1);
                    newTable.append(row2);
                }
            }
        }
        return newTable;
    }

    private Table getTriplestoreResults(StresstestResultReference ref) {
        if (ref instanceof CSVResultPath csvPath) {
            final var path = Paths.get(csvPath.csvResultPath);
            final var resultCountPath = path.resolve("result-count-data").resolve("result-count.csv");
            var resultCount = Table.read().csv(resultCountPath.toString());

            final var executionStatsPath = path.resolve("each-execution-worker-0.csv");
            var executionStats = Table.read().csv(executionStatsPath.toString());
            executionStats = executionStats.selectColumns("queryID", "run", "responseBodyHash");

            return resultCount.joinOn("responseBodyHash").fullOuter(executionStats);
        } else if (ref instanceof TaskID taskId) {
            throw new UnsupportedOperationException("StresstestResultReference of type TaskID is not yet supported.");
        } else {
            throw new IllegalArgumentException("Unknown StresstestResultReference type.");
        }
    }

    @Override
    public String getTaskName() { return "validation"; }
}
