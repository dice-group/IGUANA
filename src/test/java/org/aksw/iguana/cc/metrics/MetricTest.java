package org.aksw.iguana.cc.metrics;

import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.mockup.MockupConnection;
import org.aksw.iguana.cc.mockup.MockupQueryHandler;
import org.aksw.iguana.cc.mockup.MockupWorker;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class MetricTest {

    private int queryId = 0;
    private int workerId = 0;
    private Instant timestamp = Instant.now();

    protected HttpWorker.ExecutionStats createSuccessfulResult(int queryId, Duration duration) {
        return new HttpWorker.ExecutionStats(
                queryId,
                timestamp,
                duration,
                Optional.of(200),
                OptionalLong.of(123),
                OptionalLong.of(queryId),
                Optional.empty());
    }

    protected HttpWorker.ExecutionStats createFailedResult(int queryId, Duration duration) {
        return new HttpWorker.ExecutionStats(
                queryId,
                timestamp,
                duration,
                Optional.of(200),
                OptionalLong.of(123),
                OptionalLong.of(queryId),
                Optional.of(new Exception()));
    }

    protected HttpWorker.ExecutionStats createTimeoutResult(int queryId, Duration duration) {
        return new HttpWorker.ExecutionStats(
                queryId,
                timestamp,
                duration,
                Optional.of(200),
                OptionalLong.of(123),
                OptionalLong.of(queryId),
                Optional.of(new TimeoutException()));
    }

    protected List<HttpWorker.ExecutionStats> queryResults(int queryId, List<Duration> durations, int successful, int timeouts, int failed) {
        final var out = new ArrayList<HttpWorker.ExecutionStats>();
        for (int i = 0; i < successful; i++) {
            out.add(createSuccessfulResult(queryId, durations.get(i)));
        }
        for (int i = 0; i < timeouts; i++) {
            out.add(createTimeoutResult(queryId, durations.get(successful + i)));
        }
        for (int i = 0; i < failed; i++) {
            out.add(createFailedResult(queryId, durations.get(successful + timeouts + i)));
        }
        return out;
    }

    protected MockupWorker getWorker(int workerId) {
        final var config = new MockupWorker.Config(
                new HttpWorker.QueryMixes(10),
                "",
                2,
                false,
                new MockupQueryHandler(0, 2),
                MockupConnection.createConnectionConfig("conn", "dataset", "endpoint"),
                Duration.ofSeconds(30));
        return new MockupWorker(workerId, config);
    }
}
