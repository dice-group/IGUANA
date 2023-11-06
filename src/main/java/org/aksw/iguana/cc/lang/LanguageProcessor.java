package org.aksw.iguana.cc.lang;

import org.aksw.iguana.cc.storage.Storable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.AnnotatedTypeScanner;

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

    final private static Logger LOGGER = LoggerFactory.getLogger(LanguageProcessor.class);

    static {
        final var scanner = new AnnotatedTypeScanner(false, ContentType.class);
        final var langProcessors = scanner.findTypes("org.aksw.iguana.cc.lang");
        for (Class<?> langProcessor : langProcessors) {
            String contentType = langProcessor.getAnnotation(ContentType.class).value();
            if (LanguageProcessor.class.isAssignableFrom(langProcessor)) {
                processors.put(contentType, (Class<? extends LanguageProcessor>) langProcessor);
            } else {
                LOGGER.error("Found a class with the ContentType annotation, that doesn't inherit from the class LanguageProcessor: {}", langProcessor.getName());
            }
        }
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
