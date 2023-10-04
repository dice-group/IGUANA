package org.aksw.iguana.cc.worker;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.query.selector.QuerySelector;
import org.aksw.iguana.cc.tasks.impl.Stresstest;
import org.aksw.iguana.cc.worker.impl.SPARQLProtocolWorker;

import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
        Integer number();

        /**
         * Determines whether the results should be parsed based on the acceptHeader.
         *
         * @return true if the results should be parsed, false otherwise
         */
        Boolean parseResults();

        QueryHandler queries();

        ConnectionConfig connection();

        Duration timeout();
    }

    public record ExecutionStats(
            int queryID,
            Instant startTime,
            Duration duration, // should always exist
            Optional<Integer> httpStatusCode,
            OptionalLong contentLength,
            OptionalLong responseBodyHash,
            Optional<Exception> error
    ) {
        public enum END_STATE {
            SUCCESS(1), // TODO: values are the same as previous implementation for backwards compatibility, I think descriptive String values would be better
            TIMEOUT(-1),
            HTTP_ERROR(-2),
            MISCELLANEOUS_EXCEPTION(0);

            public final int value;
            END_STATE(int value) {
                this.value = value;
            }
        }

        public END_STATE endState() {
            if (successful()) {
                return END_STATE.SUCCESS;
            } else if (timeout()) {
                return END_STATE.TIMEOUT;
            } else if (httpError()) {
                return END_STATE.HTTP_ERROR;
            } else {
                return END_STATE.MISCELLANEOUS_EXCEPTION;
            }
        }

        public boolean completed() {
            return httpStatusCode().isPresent();
        }

        public boolean successful() {
            if (completed() && error().isEmpty()) {
                return httpStatusCode().get() / 100 == 2;
            } else {
                return false;
            }
        }

        public boolean timeout() {
            boolean timeout = false;
            if (!successful() && error().isPresent()) {
                timeout |= error().get() instanceof java.util.concurrent.TimeoutException;
                if (error().get() instanceof ExecutionException exec) {
                    timeout |= exec.getCause() instanceof HttpTimeoutException;
                }
            }
            return timeout;
        }

        public boolean httpError() {
            return httpStatusCode().isPresent() && httpStatusCode().orElse(200) / 100 != 2;
        }

        public boolean miscellaneousException() {
            return error().isPresent() && !timeout() && !httpError() && !successful();
        }
    }

    public record Result(long workerID, List<ExecutionStats> executionStats) {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TimeLimit.class),
            @JsonSubTypes.Type(value = QueryMixes.class)
    })
    sealed public interface CompletionTarget permits TimeLimit, QueryMixes {}

    public record TimeLimit(@JsonProperty(required = true) Duration duration) implements CompletionTarget {}

    public record QueryMixes(@JsonProperty(required = true) int number) implements CompletionTarget {}

    final protected long workerID;
    final protected Config config;
    final protected ResponseBodyProcessor responseBodyProcessor;
    final protected QuerySelector querySelector;

    public HttpWorker(long workerID, ResponseBodyProcessor responseBodyProcessor, Config config) {
        this.workerID = workerID;
        this.responseBodyProcessor = responseBodyProcessor;
        this.config = config;
        this.querySelector = this.config.queries().getQuerySelectorInstance();
    }

    public static String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    public abstract CompletableFuture<Result> start();

    public Config config() {
        return this.config;
    }

    public long getWorkerID() {
        return this.workerID;
    }
}
