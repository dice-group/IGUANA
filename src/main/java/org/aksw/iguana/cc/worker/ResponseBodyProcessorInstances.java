package org.aksw.iguana.cc.worker;

import java.util.HashMap;
import java.util.Map;

public class ResponseBodyProcessorInstances {
    final private Map<String, ResponseBodyProcessor> processors = new HashMap<>();

    public ResponseBodyProcessor getProcessor(String contentType) {
        if (!processors.containsKey(contentType))
            processors.put(contentType, new ResponseBodyProcessor(contentType));
        return processors.get(contentType);
    }
}
