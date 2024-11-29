package com.dua3.cabe.processor.test.config;

import java.util.Formatter;

public class TestClassStdAssert {

    public static void main(String[] args) {
        System.out.println(new TestClassStdAssert().check());
    }

    public String check() {
        try (Formatter fmt = new Formatter()) {
            String format = "%-20s: %s%n";
            fmt.format(format, "assertions enabled", TestClassStdAssert.class.desiredAssertionStatus());
            fmt.format(format, "privateNullable", check(() -> privateNullable(null)));
            fmt.format(format, "privateNonNull", check(() -> privateNonNull(null)));
            fmt.format(format, "publicNullable", check(() -> publicNullable(null)));
            fmt.format(format, "publicNonNull", check(() -> publicNonNull(null)));
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

    private String privateNullable(Object arg) {
        return "privateNullable(" + arg + ")";
    }

    private String privateNonNull(Object arg) {
        assert arg != null;
        return "privateNonNull(" + arg + ")";
    }

    public String publicNullable(Object arg) {
        return "publicNullable(" + arg + ")";
    }

    public String publicNonNull(Object arg) {
        if (arg == null) {
            throw new NullPointerException();
        }
        return "publicNonNull(" + arg + ")";
    }
}
