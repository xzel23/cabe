package com.dua3.cabe.processor.test.config;

import org.jspecify.annotations.NonNull;

import java.util.Formatter;

public interface TestInterface {

    public static void main(String[] args) {
        System.out.println(check());
    }

    public static String check() {
        try (Formatter fmt = new Formatter()) {
            String format = "%-20s: %s%n";
            TestInterface inst = new TestInterface() {};
            fmt.format(format, "assertions enabled", TestClass.class.desiredAssertionStatus());
            fmt.format(format, "privateNullable", check(() -> privateNullable(null)));
            fmt.format(format, "privateNonNull", check(() -> privateNonNull(null)));
            fmt.format(format, "publicNullable", check(() -> publicNullable(null)));
            fmt.format(format, "publicNonNull", check(() -> publicNonNull(null)));
            fmt.format(format, "publicNullableDefault", check(() -> inst.publicNullableDefault(null)));
            fmt.format(format, "publicNonNullDefault", check(() -> inst.publicNonNullDefault(null)));
            return fmt.toString();
        }
    }

    private static String check(Runnable task) {
        try {
            task.run();
            return "-";
        } catch (Throwable t) {
            return t.getClass().getName();
        }
    }

    private static String privateNullable(Object arg) {
        return "privateNullable(" + arg + ")";
    }

    private static String privateNonNull(@NonNull Object arg) {
        return "privateNonNull(" + arg + ")";
    }

    public static String publicNullable(Object arg) {
        return "publicNullable(" + arg + ")";
    }

    public static String publicNonNull(@NonNull Object arg) {
        return "publicNonNull(" + arg + ")";
    }

    public default String publicNullableDefault(Object arg) {
        return "publicNullable(" + arg + ")";
    }

    public default String publicNonNullDefault(@NonNull Object arg) {
        return "publicNonNull(" + arg + ")";
    }
}
