package org.aksw.iguana.cc.lang;

import org.aksw.iguana.cc.storage.Storable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;


/**
 * Interface for abstract language processors that work on InputStreams.
 * LanguageProcessors are used to process the content of an InputStream and extract relevant information.
 * They are used by the Worker to process the response of a request. <br>
 * LanguageProcessors must be registered in the static block of this class.
 */
public abstract class LanguageProcessor {

    /**
     * Provides the content type that a LanguageProcessor consumes.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ContentType {
        String value();
    }

    public interface LanguageProcessingData extends Storable {
        long hash();
        Class<? extends LanguageProcessor> processor();
    }

    public abstract LanguageProcessingData process(InputStream inputStream, long hash);

    final private static Map<String, Class<? extends LanguageProcessor>> processors = new HashMap<>();

    final private static Logger LOGGER = LoggerFactory.getLogger(LanguageProcessor.class);

    // Register all available LanguageProcessors here.
    static {
        processors.put("application/sparql-results+json", org.aksw.iguana.cc.lang.impl.SaxSparqlJsonResultCountingParser.class);
    }

    public static LanguageProcessor getInstance(String contentType) {
        Class<? extends LanguageProcessor> processorClass = processors.get(contentType);
        if (processorClass != null) {
            try {
                return processorClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalArgumentException("No LanguageProcessor for ContentType " + contentType);
    }

}
