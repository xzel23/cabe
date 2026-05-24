package com.dua3.cabe.processor.test.instrument;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

@SuppressWarnings({"java:S106", "java:S1192"})
public class LambdaBootstrap {

    public static void test() {
        check(() -> oneNonNullAnnotatedArgument("hello world!"), null);
        check(() -> oneNonNullAnnotatedArgument(null), "assertion failed: (arg|arg#1) is null");

        check(() -> nonNullAnnotatedSecondArgument("hello", "world"), null);
        check(() -> nonNullAnnotatedSecondArgument(null, "world"), null);
        check(() -> nonNullAnnotatedSecondArgument("hello", null), "assertion failed: (arg2|arg#2) is null");
        check(() -> nonNullAnnotatedSecondArgument(null, null), "assertion failed: (arg2|arg#2) is null");
    }

    private static void oneNonNullAnnotatedArgument(@NonNull String arg) {
        System.out.println(arg);
    }

    private static void nonNullAnnotatedSecondArgument(String arg1, @NonNull String arg2) {
        System.out.format("%s %s%n", arg1, arg2);
    }

    private static void check(Runnable task, String expectedExceptionMessage) {
        String assertionMessage = null;
        try {
            task.run();
        } catch (AssertionError ae) {
            assertionMessage = "assertion failed: " + ae.getMessage();
        }

        if (!Objects.equals(assertionMessage, expectedExceptionMessage)
                && !String.valueOf(assertionMessage).matches(String.valueOf(expectedExceptionMessage))) {
            String msg = String.format("expected: %s%nactual:   %s%n", expectedExceptionMessage, assertionMessage);
            System.err.println(msg);
            throw new IllegalStateException(msg);
        }
    }
}
