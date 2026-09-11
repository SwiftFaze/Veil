package com.swiftfaze.veil.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a reference that is permitted to be null. Used by NullAway to enforce
 * non-null-by-default semantics. Hand-rolled rather than imported from JSR-305
 * or checker-framework to avoid adding new transitive dependencies.
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
@Retention(RetentionPolicy.CLASS)
public @interface Nullable {
}
