package org.aksw.iguana.commons.reflect;

import org.aksw.iguana.commons.annotation.Shorthand;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maps the shorthand to the class names at the beginning of it's initialization.
 * Thus it has to be done once.
 *
 */
public class ShorthandMapper {

    public Logger LOGGER = LoggerFactory.getLogger(getClass());

    private Map<String, String> shortMap = new HashMap<String, String>();

    private static ShorthandMapper instance;

    public static ShorthandMapper getInstance(){
        if(instance==null){
            instance = new ShorthandMapper();
        }
        return instance;
    }


    public ShorthandMapper(){
        this("org");
    }

    /**
     * create mapping, but only searches in packages with the prefix
     * @param prefix package prefix to check
     */
    public ShorthandMapper(String prefix){
        try {
            Configuration config = ConfigurationBuilder.build(prefix).addScanners(new TypeAnnotationsScanner()).addScanners(new SubTypesScanner());
            Reflections reflections = new Reflections(new String[]{"", prefix});

            Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(Shorthand.class);
            LOGGER.info("Found {} annotated classes", annotatedClasses.size());
            LOGGER.info("Annotated Classes : {}", annotatedClasses.toString());
            ClassLoader cloader = ClassLoader.getSystemClassLoader();
            for (Class<?> annotatedClass : annotatedClasses) {
                Shorthand annotation = (Shorthand) annotatedClass.getAnnotation(Shorthand.class);
                if (annotation == null) {
                    continue;
                }
                if (shortMap.containsKey(annotation.value())) {
                    LOGGER.warn("Shorthand Key {} for Class {} already exists, pointing to Class {}. ", annotation.value(), shortMap.get(annotation.value()), annotatedClass.getCanonicalName());
                }
                shortMap.put(annotation.value(), annotatedClass.getCanonicalName());
            }
        }catch(Exception e){
            LOGGER.error("Could not create shorthand mapping", e);
        }
    }

    public Map<String, String> getShortMap() {
        return shortMap;
    }
}
