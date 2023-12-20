package com.dua3.cabe.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a parameter as accepting null-values.
 * <p>
 * This annotation is used to mark exceptions in a {@link NotNullApi} context.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface Nullable {
}
