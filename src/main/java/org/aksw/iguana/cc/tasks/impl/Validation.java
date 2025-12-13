package org.aksw.iguana.cc.tasks.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.storage.Storage;
import org.aksw.iguana.cc.storage.impl.CSVStorage;
import org.aksw.iguana.cc.tasks.Task;
import org.aksw.iguana.cc.worker.ResponseBodyProcessorInstances;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.Table;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Validation implements Task {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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

    private CSVStorage csvStorage;

    public Validation(String suiteID, long taskId, Validation.Config config, ResponseBodyProcessorInstances responseBodyProcessorInstances, List<Storage> storages, List<Metric> metrics) {
        this.config = config;
        for (Storage storage : storages) {
            if (storage instanceof CSVStorage) {
                this.csvStorage = (CSVStorage) storage;
                return;
            }
        }
    }

    @Override
    public void run() {
        final var compareColumns = new String[]{ "results", "bindings", "variables", "links" };
        final var keyColumns = new String[]{ "queryID", "run" };

        var referenceTable = prepareTable(getTriplestoreResults(config.groundTruth.resultParsingStresstest), config.groundTruth.triplestoreName, keyColumns, compareColumns);
        for (TriplestoreTuple validateTuple : config.validate) {
            var validateTable = prepareTable(getTriplestoreResults(validateTuple.resultParsingStresstest), validateTuple.triplestoreName, keyColumns, compareColumns);
            var combinedTable = referenceTable.joinOn(keyColumns).fullOuter(validateTable);

            // check every row for differences
            for (var row : combinedTable) {
                for (var columnName : compareColumns) {
                    final var queryID = row.getObject("queryID");
                    final var run = row.getObject("run");

                    final var referenceColumnName = config.groundTruth.triplestoreName + "_" + columnName;
                    final var validateColumnName = validateTuple.triplestoreName + "_" + columnName;

                    final var referenceValue = row.isMissing(referenceColumnName) ? null : row.getObject(referenceColumnName);
                    final var validateValue = row.isMissing(validateColumnName) ? null : row.getObject(validateColumnName);

                    if (referenceValue == null && validateValue == null) { continue; } // both are null, consider equal
                    if (referenceValue == null) { continue; } // missing in reference, ignore
                    if (referenceValue != null && validateValue == null) {
                        logger.warn("Triplestore \"{}\" is missing \"{}\" value for queryID {} run {}: expectedValue={}",
                                validateTuple.triplestoreName, columnName, queryID, run, referenceValue);
                    } else if (!referenceValue.equals(validateValue)) {
                        logger.warn("Triplestore \"{}\" has mismatching \"{}\" value for queryID {} run {}: actualValue={}, expectedValue={}",
                                validateTuple.triplestoreName, columnName, queryID, run, validateValue, referenceValue);
                    }
                }
            }
        }

    }

    private Table prepareTable(Table table, String triplestoreName, String[] keyColumns, String[] compareColumns) {
        // sort by key columns
        table = table.sortAscendingOn(keyColumns);

        // drop unused columns
        final var selectedColumns = Arrays.copyOf(keyColumns, keyColumns.length + compareColumns.length);
        System.arraycopy(compareColumns, 0, selectedColumns, keyColumns.length, compareColumns.length);
        table = table.selectColumns(selectedColumns);

        // add triplestore prefix to column names that are used for comparison
        for (var column : table.columns()) {
            if (Arrays.asList(compareColumns).contains(column.name())) {
                var newName = triplestoreName + "_" + column.name();
                column.setName(newName);
            }
        }
        return table;
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
