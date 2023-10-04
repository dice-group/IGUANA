package org.aksw.iguana.cc.lang;

import org.aksw.iguana.cc.lang.impl.SaxSparqlJsonResultCountingParser;
import org.aksw.iguana.cc.storage.Storable;

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

    static {
        processors.put("application/sparql-results+json", SaxSparqlJsonResultCountingParser.class);
        // TODO: find better solution, because the code underneath doesn't work
//        for (LanguageProcessor processor : ServiceLoader.load(LanguageProcessor.class)) {
//            LanguageProcessor.ContentType contentType = processor.getClass().getAnnotation(LanguageProcessor.ContentType.class);
//            if (contentType != null) {
//                processors.put(contentType.value(), processor.getClass());
//            }
//        }
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
