package com.dua3.cabe.processor.test.instrument.api.nullunmarked;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public class NullUnmarkedPackage {

    public static void test() {
        // check processing of annotated arguments
        check(() -> unannotatedArgument("hello world!"), "hello world!", null);
        check(() -> unannotatedArgument(null), null, null);

        // NonNull
        check(() -> oneNonNullAnnotatedArgument("hello world!"), "hello world!", null);
        check(() -> oneNonNullAnnotatedArgument(null), null, "assertion failed: (arg|arg#1) is null");

        check(() -> twoNonNullAnnotatedArguments("hello", "world!"), "hello world!", null);
        check(() -> twoNonNullAnnotatedArguments(null, "world!"), null, "assertion failed: (arg1|arg#1) is null");
        check(() -> twoNonNullAnnotatedArguments("hello", null), null, "assertion failed: (arg2|arg#2) is null");
        check(() -> twoNonNullAnnotatedArguments(null, null), null, "assertion failed: (arg1|arg#1) is null");

        check(() -> firstArgumentNonNullAnnotated("hello", "world!"), "hello world!", null);
        check(() -> firstArgumentNonNullAnnotated(null, "world!"), null, "assertion failed: (arg1|arg#1) is null");
        check(() -> firstArgumentNonNullAnnotated("hello", null), "hello null", null);
        check(() -> firstArgumentNonNullAnnotated(null, null), null, "assertion failed: (arg1|arg#1) is null");

        check(() -> secondArgumentNonNullAnnotated("hello", "world!"), "hello world!", null);
        check(() -> secondArgumentNonNullAnnotated(null, "world!"), "null world!", null);
        check(() -> secondArgumentNonNullAnnotated("hello", null), null, "assertion failed: (arg2|arg#2) is null");
        check(() -> secondArgumentNonNullAnnotated(null, null), null, "assertion failed: (arg2|arg#2) is null");

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

        // check that enum constructors work
        check(() -> F.WITH_INITIALISER_1.toString(), null, "assertion failed: (txt|arg#2) is null");
        check(() -> G.WITH_INITIALISER_1.toString(), null, null);
        check(() -> H.WITH_INITIALISER_1.toString(), null, null);
    }

    private static String unannotatedArgument(String arg) {
        System.out.println("oneArgument: " + arg);
        return arg;
    }

    private static String oneNonNullAnnotatedArgument(@NonNull String arg) {
        System.out.println("oneNonNullAnnotatedArgument: " + arg);
        return arg;
    }

    // @NonNull

    private static String twoNonNullAnnotatedArguments(@NonNull String arg1, @NonNull String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("twoNonNullAnnotatedArguments: " + s);
        return s;
    }

    private static String firstArgumentNonNullAnnotated(@NonNull String arg1, String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("firstArgumentNonNullAnnotated: " + s);
        return s;
    }

    private static String secondArgumentNonNullAnnotated(String arg1, @NonNull String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("secondArgumentNonNullAnnotated: " + s);
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

        if (assertionMessage != expectedExceptionMesssage && !String.valueOf(assertionMessage).matches(String.valueOf(expectedExceptionMesssage))) {            System.err.format("expected exception: %s%nactual:   %s%n", expectedExceptionMesssage, assertionMessage);
            throw new IllegalStateException();
        }

        if (!Objects.equals(result, expectedResult)) {
            System.err.format("expected result:    %s%nactual:   %s%n", expectedResult, result);
            throw new IllegalStateException();
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

        B(@NonNull String a, @NonNull String b) {
            super(a);
            this.b = b;
        }

        public String toString() {
            return super.toString() + b;
        }
    }

    enum F {
        WITH_INITIALISER_1(1, "one"),
        WITH_INITIALISER_2(2, null);

        F(int i, @NonNull String txt) {
            // nop
        }
    }

    enum G {
        WITH_INITIALISER_1(1, "one"),
        WITH_INITIALISER_2(2, null);

        G(int i, @Nullable String nullableTxt) {
            // nop
        }
    }

    enum H {
        WITH_INITIALISER_1(1, "one"),
        WITH_INITIALISER_2(2, null);

        H(int i, String unannotatedTxt) {
            // nop
        }
    }
}
