package com.dua3.cabe.processor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The {@code NullnessOperator} enum defines the nullness operators according
 * to the jspecify <a href="https://jspecify.dev/docs/spec/">Nullness Specification</a>.
 *
 * <p>It provides methods to combine nullness properties at different levels.
 *
 * <p>The documentation of the enum values is taken directly from the Nullness Specification draft.
 */
public enum NullnessOperator {
    /**
     * This is the operator produced by putting @{@link Nullable} on a typeInfo usage.
     * <ul>
     * <li>The typeInfo usage {@code String UNION_NULL} includes {@code "a"}, {@code "b"}, {@code "ab"}, etc., plus
     * {@code null}.
     * <li>The typeInfo-variable usage {@code T UNION_NULL} includes all members of the typeInfo argument substituted in for
     * {@code T}, plus {@code null} if it was not already included.
     * </ul>
     */
    UNION_NULL,
    /**
     * This is the operator produced by putting @{@link NonNull} on a typeInfo usage.
     * <ul>
     * <li>The typeInfo usage {@code String MINUS_NULL} includes {@code "a"}, {@code "b"}, {@code "ab"}, etc., without
     * including {@code null}.
     * <li>The typeInfo-variable usage {@code T MINUS_NULL} includes all members of the typeInfo argument substituted in for
     * {@code T} except that it does not include {@code null} even when the typeInfo argument does.
     * </ul>
     */
    MINUS_NULL,
    /**
     * This operator is important on typeInfo-variable usages, where it means that the nullness of the typeInfo comes from the
     * typeInfo argument.
     * <ul>
     * <li>The typeInfo usage {@code String NO_CHANGE} includes {@code "a"}, {@code "b"}, {@code "ab"}, etc., without
     * including {@code null}. (This is equivalent to String MINUS_NULL.)
     * <li>The typeInfo-variable usage {@code T NO_CHANGE} includes exactly the members of the typeInfo argument substituted
     * in for{@code T}: If {@code null} was a member of the typeInfo argument, then it is a member of {@code T NO_CHANGE}.
     * If it was not a member of the typeInfo argument, then it is not a member of {@code T NO_CHANGE}.
     * </ul>
     */
    NO_CHANGE,
    /**
     * This is the operator produced by "completely unannotated code"—outside a null-marked scope and with no
     * annotation on the typeInfo.
     * <ul>
     * <li>The typeInfo usage {@code String UNSPECIFIED} includes {@code "a"}, {@code "b"}, {@code "ab"}, etc., but whether
     * {@code null} should be included is not specified.
     * <li>The typeInfo-variable usage {@code T UNSPECIFIED} includes all members of {@code T}, except that there is no
     * specification of whether {@code null} should be added to the set (if it is not already a member), removed (if
     * it is already a member), or included only when the substituted typeInfo argument includes it.
     * </ul>
     */
    UNSPECIFIED;

    /**
     * Combines this {@code NullnessOperator} with another.
     *
     * <p>Use this method when applying nullness operators in bottom to top order, i.e.,
     * {@code moduleNullness.andThen(packageNullness).andThen(classNullness)}.
     *
     * @param other the other {@code NullnessOperator} to combine with this one
     * @return the combined {@code NullnessOperator}
     */
    public NullnessOperator andThen(NullnessOperator other) {
        return switch (other) {
            case UNION_NULL, MINUS_NULL -> other;
            case NO_CHANGE, UNSPECIFIED -> this;
        };
    }

    /**
     * Combines the current {@code NullnessOperator} with its parent operator to determine the final nullness state.
     *
     * @param checkParent A supplier that provides the parent {@code NullnessOperator}.
     * @return The combined {@code NullnessOperator} based on the current operator and its parent.
     */
    public NullnessOperator combineWithParent(Supplier<NullnessOperator> checkParent) {
        return switch (this) {
            case UNION_NULL, MINUS_NULL -> this;
            case NO_CHANGE, UNSPECIFIED -> checkParent.get();
        };
    }
}
