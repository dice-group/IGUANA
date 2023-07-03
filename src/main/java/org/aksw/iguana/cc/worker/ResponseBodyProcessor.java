package org.aksw.iguana.cc.worker;

import org.aksw.iguana.cc.lang2.LanguageProcessor;
import org.aksw.iguana.commons.io.BigByteArrayInputStream;
import org.aksw.iguana.commons.io.BigByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.*;

public class ResponseBodyProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseBodyProcessor.class);

    public ResponseBodyProcessor(LanguageProcessor languageProcessor, int threads) {
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
        this.languageProcessor = languageProcessor;
    }

    public record Key(long contentLength, long xxh64) {
    }

    private final ConcurrentHashMap.KeySetView<Key, Boolean> seenResponseBodies = ConcurrentHashMap.newKeySet();

    private final ConcurrentHashMap<Key, LanguageProcessor.LanguageProcessingData> responseDataMetrics = new ConcurrentHashMap<>();
    private final LanguageProcessor languageProcessor;

    ThreadPoolExecutor executor;


    public boolean add(long contentLength, long xxh64, BigByteArrayOutputStream bbaos) {
        final var key = new Key(contentLength, xxh64);
        if (!seenResponseBodies.contains(key)) {
            final var added = seenResponseBodies.add(key);
            if (added) {
                submit(key, bbaos);
                return true;
            }
        }
        return false; // TODO: reuse bbaos in this case
    }

    private void submit(Key key, BigByteArrayOutputStream bigByteArrayOutputStream) {
        executor.execute(() -> {
            var processingResult = languageProcessor.process(new BigByteArrayInputStream(bigByteArrayOutputStream));
            responseDataMetrics.put(key, processingResult);
        });
    }

    public ConcurrentHashMap<Key, LanguageProcessor.LanguageProcessingData> getResponseDataMetrics() {
        final var timeout = Duration.ofMinutes(10);
        LOGGER.info(MessageFormat.format("Shutting down ResponseBodyProcessor with {0}min timeout to finish processing.\n{1} tasks remaining.", timeout.get(ChronoUnit.MINUTES), executor.getQueue().size()));
        boolean noTimeout;
        try {
            noTimeout = executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (noTimeout) LOGGER.info("ResponseBodyProcessor completed.");
        else LOGGER.warn("ResponseBodyProcessor timed out.");
        return responseDataMetrics;
    }
}
