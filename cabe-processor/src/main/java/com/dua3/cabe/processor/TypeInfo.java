package com.dua3.cabe.processor;

/**
 * Represents metadata information about a type.
 *
 * <p>This class encapsulates a type name and its associated nullness operator.
 * The type name is represented as a string, and the nullness operator is used
 * to specify the nullability semantics associated with the type.
 *
 * @param type the class name of the parameter type
 * @param rawType the class name of the parameter type with generic type parameters removed
 * @param nullnessOperator the {@link NullnessOperator} of the parameter
 */
public record TypeInfo(String type, String rawType, NullnessOperator nullnessOperator) {
}
