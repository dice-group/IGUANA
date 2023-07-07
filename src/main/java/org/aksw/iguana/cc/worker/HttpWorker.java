package org.aksw.iguana.cc.worker;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.aksw.iguana.cc.tasks.impl.Stresstest;
import org.aksw.iguana.cc.worker.impl.SPARQLProtocolWorker;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for the Worker Thread used in the {@link Stresstest}
 *
 * @author f.conrads
 */
public abstract class HttpWorker {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SPARQLProtocolWorker.Config.class, name = "SPARQLProtocolWorker"),
    })
    public interface Config {
        // TODO: add delay or complete after
        CompletionTarget completionTarget();

        String acceptHeader();

        /**
         * Returns the number of workers with this configuration that will be started.
         *
         * @return the number of workers
         */
        int number();

        /**
         * Determines whether the results should be parsed based on the acceptHeader.
         *
         * @return true if the results should be parsed, false otherwise
         */
        boolean parseResults();
    }

    public record ExecutionStats(Instant startTime,
                                 Optional<Duration> duration,
                                 int httpStatusCode,
                                 long contentLength,
                                 Long responseBodyHash,
                                 Exception error
    ) {
    }

    public record Result(long workerID, List<ExecutionStats> executionStats) {
    }

    sealed public interface CompletionTarget permits TimeLimit, QueryMixes {
    }

    public record TimeLimit(Duration d) implements CompletionTarget {
    }

    public record QueryMixes(int n) implements CompletionTarget {
    }

    final protected long workerId;
    final protected Config config;
    final protected ResponseBodyProcessor responseBodyProcessor;

    public HttpWorker(long workerId, ResponseBodyProcessor responseBodyProcessor, Config config) {
        this.workerId = workerId;
        this.responseBodyProcessor = responseBodyProcessor;
        this.config = config;
    }

    public abstract CompletableFuture<Result> start();

}
