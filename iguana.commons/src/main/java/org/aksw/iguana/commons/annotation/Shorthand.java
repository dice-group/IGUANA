package org.aksw.iguana.commons.annotation;

import java.lang.annotation.*;

/**
 * Sets a short name to be used in the TypedFactory instead of the whole class name
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Shorthand {

    String value();
}
