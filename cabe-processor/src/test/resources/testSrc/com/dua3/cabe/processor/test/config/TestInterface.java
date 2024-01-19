package com.dua3.cabe.processor.test.config;

import com.dua3.cabe.annotations.NotNull;

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
            fmt.format(format, "privateNotNull", check(() -> privateNotNull(null)));
            fmt.format(format, "publicNullable", check(() -> publicNullable(null)));
            fmt.format(format, "publicNotNull", check(() -> publicNotNull(null)));
            fmt.format(format, "publicNullableDefault", check(() -> inst.publicNullableDefault(null)));
            fmt.format(format, "publicNotNullDefault", check(() -> inst.publicNotNullDefault(null)));
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

    private static String privateNotNull(@NotNull Object arg) {
        return "privateNotNull(" + arg + ")";
    }

    public static String publicNullable(Object arg) {
        return "publicNullable(" + arg + ")";
    }

    public static String publicNotNull(@NotNull Object arg) {
        return "publicNotNull(" + arg + ")";
    }

    public default String publicNullableDefault(Object arg) {
        return "publicNullable(" + arg + ")";
    }

    public default String publicNotNullDefault(@NotNull Object arg) {
        return "publicNotNull(" + arg + ")";
    }
}
