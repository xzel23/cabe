package com.dua3.cabe.processor.test.config;

import com.dua3.cabe.annotations.NotNull;

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
            fmt.format(format, "privateNotNull", check(() -> privateNotNull(null)));
            fmt.format(format, "publicNullable", check(() -> publicNullable(null)));
            fmt.format(format, "publicNotNull", check(() -> publicNotNull(null)));
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

    private String privateNotNull(Object arg) {
        assert arg != null;
        return "privateNotNull(" + arg + ")";
    }

    public String publicNullable(Object arg) {
        return "publicNullable(" + arg + ")";
    }

    public String publicNotNull(Object arg) {
        if (arg == null) {
            throw new NullPointerException();
        }
        return "publicNotNull(" + arg + ")";
    }
}
