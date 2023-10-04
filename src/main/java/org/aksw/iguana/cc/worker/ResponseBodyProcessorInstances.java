package org.aksw.iguana.cc.worker;

import org.aksw.iguana.cc.lang.LanguageProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ResponseBodyProcessorInstances {
    final private Map<String, ResponseBodyProcessor> processors = new HashMap<>();

    public ResponseBodyProcessorInstances() {}

    public ResponseBodyProcessorInstances(List<ResponseBodyProcessor.Config> configs) {
        if (configs == null) return;
        for (var config : configs) {
            processors.put(config.contentType(), new ResponseBodyProcessor(config));
        }
    }

    public ResponseBodyProcessor getProcessor(String contentType) {
        if (!processors.containsKey(contentType)) {
            processors.put(contentType, new ResponseBodyProcessor(contentType));
        }
        return processors.get(contentType);
    }

    /**
     * Returns a Supplier that returns the results of all ResponseBodyProcessors. A supplier is used for data
     * abstraction.
     *
     * @return supplier for all results
     */
    public Supplier<Map<LanguageProcessor, List<LanguageProcessor.LanguageProcessingData>>> getResults() {
        return () -> { // TODO: consider removing the languageProcessor as the key, it's only used right now for creating strings for naming
            Map<LanguageProcessor, List<LanguageProcessor.LanguageProcessingData>> out = new HashMap<>();
            for (var processor : processors.values()) {
                out.put(processor.getLanguageProcessor(), processor.getResponseDataMetrics());
            }
            return out;
        };
    }
}
