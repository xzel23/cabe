package com.dua3.cabe.test.coba.api.nullable;

import com.dua3.cabe.annotations.NotNull;
import com.dua3.cabe.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public class NullablePackage {

    public static void test() {
        // check processing of annotated arguments
        check(() -> unannotatedArgument("hello world!"), "hello world!", null);
        check(() -> unannotatedArgument(null), null, null);

        // NotNull
        check(() -> oneNotNullAnnotatedArgument("hello world!"), "hello world!", null);
        check(() -> oneNotNullAnnotatedArgument(null), null, "assertion failed: arg is null");

        check(() -> twoNotNullAnnotatedArguments("hello", "world!"), "hello world!", null);
        check(() -> twoNotNullAnnotatedArguments(null, "world!"), null, "assertion failed: arg1 is null");
        check(() -> twoNotNullAnnotatedArguments("hello", null), null, "assertion failed: arg2 is null");
        check(() -> twoNotNullAnnotatedArguments(null, null), null, "assertion failed: arg1 is null");

        check(() -> firstArgumentNotNullAnnotated("hello", "world!"), "hello world!", null);
        check(() -> firstArgumentNotNullAnnotated(null, "world!"), null, "assertion failed: arg1 is null");
        check(() -> firstArgumentNotNullAnnotated("hello", null), "hello null", null);
        check(() -> firstArgumentNotNullAnnotated(null, null), null, "assertion failed: arg1 is null");

        check(() -> secondArgumentNotNullAnnotated("hello", "world!"), "hello world!", null);
        check(() -> secondArgumentNotNullAnnotated(null, "world!"), "null world!", null);
        check(() -> secondArgumentNotNullAnnotated("hello", null), null, "assertion failed: arg2 is null");
        check(() -> secondArgumentNotNullAnnotated(null, null), null, "assertion failed: arg2 is null");

        // Nullable
        check(() -> oneNullableAnnotatedArgument("hello world!"), "hello world!", null);
        check(() -> oneNullableAnnotatedArgument(null), null, null);

        check(() -> twoNullableAnnotatedArguments("hello", "world!"), "hello world!", null);
        check(() -> twoNullableAnnotatedArguments(null, "world!"), "null world!", null);
        check(() -> twoNullableAnnotatedArguments("hello", null), "hello null", null);
        check(() -> twoNullableAnnotatedArguments(null, null), "null null", null);

        check(() -> firstArgumentNullableAnnotated("hello", "world!"), "hello world!", null);
        check(() -> firstArgumentNullableAnnotated(null, "world!"), "null world!", null);
        check(() -> firstArgumentNullableAnnotated("hello", null), "hello null", null);
        check(() -> firstArgumentNullableAnnotated(null, null), "null null", null);

        check(() -> secondArgumentNullableAnnotated("hello", "world!"), "hello world!", null);
        check(() -> secondArgumentNullableAnnotated(null, "world!"), "null world!", null);
        check(() -> secondArgumentNullableAnnotated("hello", null), "hello null", null);
        check(() -> secondArgumentNullableAnnotated(null, null), "null null", null);

        // record parameter
        check(() -> Pair.of("A", 1).toString(), "Pair[first=A, second=1]", null);

        // check that annotated arguments to constructors work
        assert new B("hello", " world!").toString().equals("hello world!");
    }

    private static String unannotatedArgument(String arg) {
        System.out.println("oneArgument: " + arg);
        return arg;
    }

    private static String oneNotNullAnnotatedArgument(@NotNull String arg) {
        System.out.println("oneNotNullAnnotatedArgument: " + arg);
        return arg;
    }

    // @NotNull

    private static String twoNotNullAnnotatedArguments(@NotNull String arg1, @NotNull String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("twoNotNullAnnotatedArguments: " + s);
        return s;
    }

    private static String firstArgumentNotNullAnnotated(@NotNull String arg1, String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("firstArgumentNotNullAnnotated: " + s);
        return s;
    }

    private static String secondArgumentNotNullAnnotated(String arg1, @NotNull String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("secondArgumentNotNullAnnotated: " + s);
        return s;
    }

    private static String oneNullableAnnotatedArgument(@Nullable String arg) {
        System.out.println("oneNullableAnnotatedArgument: " + arg);
        return arg;
    }

    // @Nullable

    private static String twoNullableAnnotatedArguments(@Nullable String arg1, @Nullable String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("twoNullableAnnotatedArguments: " + s);
        return s;
    }

    private static String firstArgumentNullableAnnotated(@Nullable String arg1, String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("firstArgumentNullableAnnotated: " + s);
        return s;
    }

    private static String secondArgumentNullableAnnotated(String arg1, @Nullable String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("secondArgumentNullableAnnotated: " + s);
        return s;
    }

    private static void check(Supplier<String> task, @Nullable String expectedResult, @Nullable String expectedExceptionMesssage) {
        String assertionMessage = null;
        String result = null;
        try {
            result = task.get();
        } catch (AssertionError ae) {
            assertionMessage = "assertion failed: " + ae.getMessage();
        }

        if (!Objects.equals(assertionMessage, expectedExceptionMesssage)) {
            String msg = String.format("expected exception: %s%nactual:   %s%n", expectedExceptionMesssage, assertionMessage);
            System.err.println(msg);
            throw new IllegalStateException(msg);
        }

        if (!Objects.equals(result, expectedResult)) {
            String msg = String.format("expected result:    %s%nactual:   %s%n", expectedResult, result);
            System.err.println(msg);
            throw new IllegalStateException(msg);
        }
    }

    public record Pair<T1, T2>(T1 first, T2 second) {
        public static <T1, T2> Pair<T1, T2> of(T1 first, T2 second) {
            return new Pair<>(first, second);
        }
    }

    static class A {
        private String s;

        A(String s) {
            this.s = s;
        }

        public String toString() {
            return s;
        }
    }

    static class B extends A {
        private String b;

        B(@NotNull String a, @NotNull String b) {
            super(a);
            this.b = b;
        }

        public String toString() {
            return super.toString() + b;
        }
    }
}
