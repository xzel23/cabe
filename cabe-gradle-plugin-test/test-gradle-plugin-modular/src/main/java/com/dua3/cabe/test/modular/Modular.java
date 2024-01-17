package com.dua3.cabe.test.modular;

import com.dua3.cabe.annotations.NotNull;

import java.util.Objects;

public class Modular {

    public static void main(String[] args) {
        check(() -> oneNonNullAnnotatedArgument("hello world!"), null);
        check(() -> oneNonNullAnnotatedArgument(null), "assertion failed: arg is null");

        check(() -> nonNullAnnotatedSecondArgument("hello", "world"), null);
        check(() -> nonNullAnnotatedSecondArgument(null, "world"), null);
        check(() -> nonNullAnnotatedSecondArgument("hello", null), "assertion failed: arg2 is null");
        check(() -> nonNullAnnotatedSecondArgument(null, null), "assertion failed: arg2 is null");

        check(() -> twoNonNullAnnotatedArgument("hello", "world"), null);
        check(() -> twoNonNullAnnotatedArgument(null, "world"), "assertion failed: arg1 is null");
        check(() -> twoNonNullAnnotatedArgument("hello", null), "assertion failed: arg2 is null");
        check(() -> twoNonNullAnnotatedArgument(null, null), "assertion failed: arg1 is null");
    }

    private static void oneNonNullAnnotatedArgument(@NotNull String arg) {
        System.out.println(arg);
    }

    private static void nonNullAnnotatedSecondArgument(String arg1, @NotNull String arg2) {
        System.out.format("%s %s%n", arg1, arg2);
    }

    private static void twoNonNullAnnotatedArgument(@NotNull String arg1, @NotNull String arg2) {
        System.out.format("%s %s%n", arg1, arg2);
    }

    private static void check(Runnable task, String expectedExceptionMesssage) {
        String assertionMessage = null;
        try {
            task.run();
        } catch (AssertionError ae) {
            assertionMessage = "assertion failed: " + ae.getMessage();
        }
        if (!Objects.equals(assertionMessage, expectedExceptionMesssage)) {
            String msg = String.format("expected: %s%nactual:   %s%n", expectedExceptionMesssage, assertionMessage);
            System.err.println(msg);
            throw new IllegalStateException(msg);
        }
    }

}
