package org.aksw.iguana.commons.reflect;

import org.aksw.iguana.commons.annotation.Shorthand;
import org.reflections.Reflections;
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
        this("");
    }

    /**
     * create mapping, but only searches in packages with the prefix
     * @param prefix package prefix to check
     */
    public ShorthandMapper(String prefix){
        Reflections reflections = new Reflections(prefix);
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(Shorthand.class);
        ClassLoader cloader = ClassLoader.getSystemClassLoader();
        for(Class<?> annotatedClass : annotatedClasses){
            Shorthand annotation = (Shorthand)annotatedClass.getAnnotation(Shorthand.class);
            if(annotation == null ){
                System.out.println();
            }
            if(shortMap.containsKey(annotation.value())){
                LOGGER.warn("Shorthand Key {} for Class {} already exists, pointing to Class {}. ", annotation.value(), shortMap.get(annotation.value()), annotatedClass.getCanonicalName());
            }
            shortMap.put(annotation.value(), annotatedClass.getCanonicalName());
        }
    }

    public Map<String, String> getShortMap() {
        return shortMap;
    }
}
