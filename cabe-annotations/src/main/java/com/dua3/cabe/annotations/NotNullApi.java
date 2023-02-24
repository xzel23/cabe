package com.dua3.cabe.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark parameters @{@link NotNull} by default for all methods contained in a class or package.
 * Use @{@link NotNull} for exceptions.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.PACKAGE, ElementType.TYPE})
public @interface NotNullApi {
}
