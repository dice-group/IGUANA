package org.aksw.iguana.commons.annotation;

import java.lang.annotation.*;

/**
 * Lets the TypeFactory know that the Parameter can be null and thus be ignored.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Nullable {
}
