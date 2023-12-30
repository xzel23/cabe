package com.dua3.cabe.test.modular;

import com.dua3.cabe.annotations.NotNull;

import java.util.Objects;

public class Modular {

    public static void main(String[] args) {
        check(() -> oneNonNullAnnotatedArgument("hello world!"), null);
        check(() -> oneNonNullAnnotatedArgument(null), "error: parameter 'arg' must not be null");

        check(() -> nonNullAnnotatedSecondArgument("hello", "world"), null);
        check(() -> nonNullAnnotatedSecondArgument(null, "world"), null);
        check(() -> nonNullAnnotatedSecondArgument("hello", null), "error: parameter 'arg2' must not be null");
        check(() -> nonNullAnnotatedSecondArgument(null, null), "error: parameter 'arg2' must not be null");

        check(() -> twoNonNullAnnotatedArgument("hello", "world"), null);
        check(() -> twoNonNullAnnotatedArgument(null, "world"), "error: parameter 'arg1' must not be null");
        check(() -> twoNonNullAnnotatedArgument("hello", null), "error: parameter 'arg2' must not be null");
        check(() -> twoNonNullAnnotatedArgument(null, null), "error: parameter 'arg1' must not be null");
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
            assertionMessage = "error: " + ae.getMessage();
        }
        if (!Objects.equals(assertionMessage, expectedExceptionMesssage)) {
            String msg = String.format("expected: %s%nactual:   %s%n", expectedExceptionMesssage, assertionMessage);
            System.err.println(msg);
            throw new IllegalStateException(msg);
        }
    }

}
