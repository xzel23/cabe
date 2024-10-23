package com.dua3.cabe.processor.test.instrument;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class ParameterAnnotations {

    public static void test() {
        new ParameterAnnotations().doTest();
    }

    public void doTest() {
        // test with generic parameters
        check(() -> new C("hello world!").toString(), "hello world!", null);
        check(() -> new C(null).toString(), null, "assertion failed: (t|arg#1) is null");

        check(() -> genericArguments("hello", "world", obj -> " " + obj + "!"), "hello world!", null);
        check(() -> genericArguments(null, "world", obj -> " " + obj + " "), null, "assertion failed: (prefix|arg#1) is null");
        check(() -> genericArguments("hello", null, obj -> " " + obj + " "), null, "assertion failed: (suffix|arg#2) is null");
        check(() -> genericArguments("hello", "world", null), null, "assertion failed: (func|arg#3) is null");

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

        check(() -> intermixedWithPrimitives(87, " hello ", 99), "87 hello 99", null);
        check(() -> intermixedWithPrimitives(87, null, 99), null, "assertion failed: (txt|arg#2) is null");

        check(() -> genericParameter(new A("hello world!")), "hello world!", null);
        check(() -> genericParameter(null), null, "assertion failed: (arg|arg#1) is null");

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
        check(() -> new MyPair("A", 1).toString(), "MyPair[first=A, second=1]", null);
        check(() -> new MyPair(null, 1).toString(), "MyPair[first=null, second=1]", null);
        check(() -> new MyPair("A", null).toString(), "MyPair[first=A, second=null]", null);

        check(() -> new NonNullRecord("A", "B").toString(), "NonNullRecord[a=A, b=B]", null);
        check(() -> new NonNullRecord(null, "B").toString(), null, "assertion failed: (a|arg#1) is null");
        check(() -> new NonNullRecord("A", null).toString(), null, "assertion failed: (b|arg#2) is null");

        // check that annotated arguments to constructors work
        assert new B("hello", " world!").toString().equals("hello world!");
        check(() -> new B(null, " world!").toString(), null, "assertion failed: (a|arg#1) is null");
        check(() -> new B("hello", null).toString(), null, "assertion failed: (b|arg#2) is null");
    }

    private String unannotatedArgument(String arg) {
        System.out.println("oneArgument: " + arg);
        return arg;
    }

    private String oneNonNullAnnotatedArgument(@NonNull String arg) {
        System.out.println("oneNonNullAnnotatedArgument: " + arg);
        return arg;
    }

    private String twoNonNullAnnotatedArguments(@NonNull String arg1, @NonNull String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("twoNonNullAnnotatedArguments: " + s);
        return s;
    }

    // @NonNull

    private String firstArgumentNonNullAnnotated(@NonNull String arg1, String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("firstArgumentNonNullAnnotated: " + s);
        return s;
    }

    private String secondArgumentNonNullAnnotated(String arg1, @NonNull String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("secondArgumentNonNullAnnotated: " + s);
        return s;
    }

    private String oneNullableAnnotatedArgument(@Nullable String arg) {
        System.out.println("oneNullableAnnotatedArgument: " + arg);
        return arg;
    }

    private String twoNullableAnnotatedArguments(@Nullable String arg1, @Nullable String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("twoNullableAnnotatedArguments: " + s);
        return s;
    }

    private String intermixedWithPrimitives(int a, @NonNull String txt, int b) {
        String s = String.format("%d%s%d", a, txt, b);
        System.out.println("intermixWithPrimitives: " + s);
        return s;
    }

    private <T> String genericParameter(@NonNull T arg) {
        String s = String.valueOf(arg);
        System.out.println("genericParameter: " + s);
        return s;
    }

    // @Nullable

    private String firstArgumentNullableAnnotated(@Nullable String arg1, String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("firstArgumentNullableAnnotated: " + s);
        return s;
    }

    private String secondArgumentNullableAnnotated(String arg1, @Nullable String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("secondArgumentNullableAnnotated: " + s);
        return s;
    }

    private void check(Supplier<String> task, @Nullable String expectedResult, @Nullable String expectedExceptionMesssage) {
        String assertionMessage = null;
        String result = null;
        try {
            result = task.get();
        } catch (AssertionError ae) {
            assertionMessage = "assertion failed: " + ae.getMessage();
        }

        if (!String.valueOf(assertionMessage).matches(String.valueOf(expectedExceptionMesssage))) {
            String msg = String.format("expected exception: %s%nactual: %s%n", expectedExceptionMesssage, assertionMessage);
            System.err.println(msg);
            throw new IllegalStateException(msg);
        }

        if (!Objects.equals(result, expectedResult)) {
            String msg = String.format("expected result: %s%nactual: %s%n", expectedResult, result);
            System.err.println(msg);
            throw new IllegalStateException(msg);
        }
    }

    public record MyPair<T1, T2>(@Nullable T1 first, @Nullable T2 second) {}

    public record NonNullRecord(@NonNull String a, @NonNull String b) {}

    class A {
        private String s;

        A(String s) {
            this.s = s;
        }

        public String toString() {
            return s;
        }
    }

    class B extends A {
        private String b;

        B(@NonNull String a, @NonNull String b) {
            super(a);
            this.b = b;
        }

        public String toString() {
            return super.toString() + b;
        }
    }

    @NullMarked
    class C<T> {
        private T t;

        C(T t) {
            this.t = t;
        }

        @Override
        public String toString() {
            return String.valueOf(t);
        }
    }

    public String genericArguments(@NonNull String prefix, @NonNull String suffix, @NonNull Function<C<? extends Object>, String> func) {
        return prefix + func.apply(new C(suffix));
    }

}
