package org.aksw.iguana.commons.annotation;

import java.lang.annotation.*;

/**
 * Uses provided names in the order of the constructor parameters, instead of the constructor parameter names for the TypeFactory
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
@Inherited
public @interface ParameterNames {

    String[] names() default "";
}
