package org.aksw.iguana.cc.worker;

import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class ResponseBodyProcessor {
    public record Config(String contentType, Integer threads, Duration timeout) {
        public Config(String contentType, Integer threads, Duration timeout) {
            this.contentType = contentType;
            this.threads = threads == null ? 1 : threads;
            this.timeout = timeout == null ? Duration.ofMinutes(10) : timeout;
        }
    }

    public record Key(long contentLength, long xxh64) {}

    public ResponseBodyProcessor(Config config) {
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(config.threads == null ? 1 : config.threads);
        this.languageProcessor = LanguageProcessor.getInstance(config.contentType);
        this.timeout = config.timeout;
    }

    public ResponseBodyProcessor(String contentType) {
        this(new Config(contentType, null, null));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseBodyProcessor.class);

    private final Duration timeout;

    private final ConcurrentHashMap.KeySetView<Key, Boolean> seenResponseBodies = ConcurrentHashMap.newKeySet();

    private final List<LanguageProcessor.LanguageProcessingData> responseDataMetrics = Collections.synchronizedList(new ArrayList<>());
    private final LanguageProcessor languageProcessor;

    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService executorHandler = Executors.newScheduledThreadPool(1);

    public boolean add(long contentLength, long xxh64, InputStream responseBodyStream) {
        final var key = new Key(contentLength, xxh64);
        if (seenResponseBodies.add(key)) {
            submit(key, responseBodyStream);
            return true;
        }
        return false;
    }

    private void submit(Key key, InputStream responseBodyStream) {
        final var future = executor.submit(() -> {
            var processingResult = languageProcessor.process(responseBodyStream, key.xxh64);
            responseDataMetrics.add(processingResult);
        });
        executorHandler.schedule(() -> {
            if (!future.isDone()) {
                future.cancel(true);
                LOGGER.warn("ResponseBodyProcessor timed out for key: {}", key);
            }
        }, timeout.toSeconds(), TimeUnit.SECONDS);
    }

    public List<LanguageProcessor.LanguageProcessingData> getResponseDataMetrics() {
        if (executor.isTerminated()) {
            return responseDataMetrics;
        }

        LOGGER.info(MessageFormat.format("Shutting down ResponseBodyProcessor with {0}min {1}s timeout to finish processing. {2} tasks remaining.",
                timeout.toMinutes(), 
                String.format("%02d", timeout.toSecondsPart()),
                executor.getQueue().size()));
        boolean noTimeout;
        try {
            executor.shutdown();
            noTimeout = executor.awaitTermination(timeout.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (noTimeout) LOGGER.info("ResponseBodyProcessor completed.");
        else LOGGER.warn("ResponseBodyProcessor timed out.");
        return responseDataMetrics;
    }

    public LanguageProcessor getLanguageProcessor() {
        return this.languageProcessor;
    }
}
