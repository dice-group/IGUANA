package org.aksw.iguana.cc.lang2;

import java.io.InputStream;

/**
 * Interface for abstract language processors that work on InputStreams.
 */
public interface AbstractLanguageProcessor {

    interface LanguageProcessingData {
        public Class<? extends AbstractLanguageProcessor> processor();
    }

    LanguageProcessingData process(InputStream inputStream);
}
