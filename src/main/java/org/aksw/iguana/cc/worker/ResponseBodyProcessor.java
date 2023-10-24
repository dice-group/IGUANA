package org.aksw.iguana.cc.worker;

import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.commons.io.BigByteArrayInputStream;
import org.aksw.iguana.commons.io.BigByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class ResponseBodyProcessor {
    public record Config(String contentType, Integer threads) {
        public Config(String contentType, Integer threads) {
            this.contentType = contentType;
            this.threads = threads == null ? 1 : threads;
        }
    }

    public record Key(long contentLength, long xxh64) {}

    public ResponseBodyProcessor(Config config) {
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(config.threads == null ? 1 : config.threads);
        this.languageProcessor = LanguageProcessor.getInstance(config.contentType);
    }

    public ResponseBodyProcessor(String contentType) {
        this(new Config(contentType, null));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseBodyProcessor.class);

    private final ConcurrentHashMap.KeySetView<Key, Boolean> seenResponseBodies = ConcurrentHashMap.newKeySet();

    private final List<LanguageProcessor.LanguageProcessingData> responseDataMetrics = Collections.synchronizedList(new ArrayList<>());
    private final LanguageProcessor languageProcessor;

    private final ThreadPoolExecutor executor;

    public boolean add(long contentLength, long xxh64, BigByteArrayOutputStream bbaos) {
        final var key = new Key(contentLength, xxh64);
        if (seenResponseBodies.add(key)) {
            submit(key, bbaos);
            return true;
        }
        return false;
    }

    private void submit(Key key, BigByteArrayOutputStream bigByteArrayOutputStream) {
        executor.execute(() -> {
            var processingResult = languageProcessor.process(new BigByteArrayInputStream(bigByteArrayOutputStream), key.xxh64);
            responseDataMetrics.add(processingResult);
        });
    }

    public List<LanguageProcessor.LanguageProcessingData> getResponseDataMetrics() {
        if (executor.isTerminated()) {
            return responseDataMetrics;
        }

        final var timeout = Duration.ofMinutes(10);
        LOGGER.info(MessageFormat.format("Shutting down ResponseBodyProcessor with {0}min timeout to finish processing. {1} tasks remaining.", timeout.toMinutes(), executor.getQueue().size()));
        boolean noTimeout;
        try {
            executor.shutdown();
            noTimeout = executor.awaitTermination(10, TimeUnit.MINUTES);
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
