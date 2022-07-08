package com.dua3.cabe.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a parameter as not accepting null-values.
 * <p> 
 * The Cabe plugin will instrument the code with an assertion that checks for all parameters annotated with `@NotNull`
 * having a non-null value. Run your code with assertions enabled to do runtime-checking.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PARAMETER)
public @interface NotNull {
}
