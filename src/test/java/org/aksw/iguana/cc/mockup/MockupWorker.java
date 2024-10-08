package org.aksw.iguana.cc.mockup;

import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.config.elements.DatasetConfig;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.worker.HttpWorker;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

public class MockupWorker extends HttpWorker {
    public record Config(
            CompletionTarget completionTarget,
            String acceptHeader,
            Integer number,
            Boolean parseResults,
            QueryHandler queries,
            ConnectionConfig connection,
            Duration timeout
    ) implements HttpWorker.Config {}


    /**
     * All values except the workerID and queries may be null. I recommend to use the MockupQueryHandler.
     * I would also recommend to set the connection, if you want to use the StresstestResultProcessor.
     */
    public MockupWorker(long workerID, CompletionTarget target, String acceptHeader, Integer number, Boolean parseResults, QueryHandler queries, ConnectionConfig connection, Duration timeout) {
        super(workerID, null, new Config(
                target,
                acceptHeader,
                number,
                parseResults,
                queries,
                connection,
                timeout
            )
        );
    }

    /**
     * All other values will be set to null. This is the bare minimum to make it work with the StresstestResultProcessor.
     */
    public MockupWorker(long workerID, QueryHandler queries, String connectionName, String connectionVersion, String datasetName, Duration timeout) {
        super(workerID, null, new Config(
                null,
                null,
                null,
                null,
                queries,
                new ConnectionConfig(connectionName, connectionVersion, new DatasetConfig(datasetName, null), null, null, null, null),
                timeout
        ));
    }

    @Override
    public CompletableFuture<Result> start() {
        return null;
    }

    public static List<Result> createWorkerResults(QueryHandler queries, List<HttpWorker> workers) {
        final var startTime = ZonedDateTime.of(2023, 10, 11, 14, 14, 10, 0, ZoneId.of("UTC"));
        final var endTime = ZonedDateTime.of(2023, 10, 12, 15, 15, 15, 0, ZoneId.of("UTC"));

        final var queryNumber = queries.getQueryCount();

        Instant time = Instant.parse("2023-10-21T20:48:06.399Z");

        final var results = new ArrayList<Result>();
        for (var worker : workers) {
            final var exectutionStats = new ArrayList<ExecutionStats>();
            for (int queryID = 0; queryID < queryNumber; queryID++) {
                // successful execution
                final var sucHttpCode = Optional.of(200);
                final var sucDuration = Duration.ofSeconds(2);
                final var sucLength = OptionalLong.of(1000);
                final var responseBodyHash = OptionalLong.of(123);
                time = time.plusSeconds(1);
                exectutionStats.add(new ExecutionStats(queryID, time, sucDuration, sucHttpCode, sucLength, responseBodyHash, Optional.empty()));

                // failed execution (http error)
                var failHttpCode = Optional.of(404);
                var failDuration = Duration.ofMillis(500);
                var failLength = OptionalLong.empty();
                var failResponseBodyHash = OptionalLong.empty();
                var failException = new Exception("httperror");
                time = time.plusSeconds(1);
                exectutionStats.add(new ExecutionStats(queryID, time, failDuration, failHttpCode, failLength, failResponseBodyHash, Optional.of(failException)));

                // failed execution
                failHttpCode = Optional.of(200);
                failDuration = Duration.ofSeconds(5);
                failLength = OptionalLong.empty();
                failResponseBodyHash = OptionalLong.of(456);
                failException = new Exception("io_exception");
                time = time.plusSeconds(1);
                exectutionStats.add(new ExecutionStats(queryID, time, failDuration, failHttpCode, failLength, failResponseBodyHash, Optional.of(failException)));
            }
            results.add(new Result(worker.getWorkerID(), exectutionStats, startTime, endTime));
        }
        return results;
    }

    public static List<HttpWorker> createWorkers(int idOffset, int workerNumber, QueryHandler queries, String connectionName, String connectionVersion, String datasetName) {
        final var workers = new ArrayList<HttpWorker>();
        for (int i = idOffset; i < workerNumber + idOffset; i++) {
            workers.add(new MockupWorker(i, queries, connectionName, connectionVersion, datasetName, Duration.ofSeconds(2)));
        }
        return workers;
    }

}
