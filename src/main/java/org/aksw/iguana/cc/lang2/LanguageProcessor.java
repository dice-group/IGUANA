package org.aksw.iguana.cc.lang2;

import java.io.InputStream;

/**
 * Interface for abstract language processors that work on InputStreams.
 */
public interface LanguageProcessor {

    interface LanguageProcessingData {
        Class<? extends LanguageProcessor> processor();
    }

    LanguageProcessingData process(InputStream inputStream);
}
