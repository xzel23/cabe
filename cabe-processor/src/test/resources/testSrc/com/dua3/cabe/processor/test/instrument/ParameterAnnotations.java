package com.dua3.cabe.processor.test.instrument;

import com.dua3.cabe.annotations.NotNull;
import com.dua3.cabe.annotations.Nullable;

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
        check(() -> new C(null).toString(), null, "assertion failed: t is null");

        check(() -> genericArguments("hello", "world", obj -> " " + obj + "!"), "hello world!", null);
        check(() -> genericArguments(null, "world", obj -> " " + obj + " "), null, "assertion failed: prefix is null");
        check(() -> genericArguments("hello", null, obj -> " " + obj + " "), null, "assertion failed: suffix is null");
        check(() -> genericArguments("hello", "world", null), null, "assertion failed: func is null");

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

        check(() -> intermixedWithPrimitives(87, " hello ", 99), "87 hello 99", null);
        check(() -> intermixedWithPrimitives(87, null, 99), null, "assertion failed: txt is null");

        check(() -> genericParameter(new A("hello world!")), "hello world!", null);
        check(() -> genericParameter(null), null, "assertion failed: arg is null");

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

        check(() -> new NotNullRecord("A", "B").toString(), "NotNullRecord[a=A, b=B]", null);
        check(() -> new NotNullRecord(null, "B").toString(), null, "assertion failed: a is null");
        check(() -> new NotNullRecord("A", null).toString(), null, "assertion failed: b is null");

        // check that annotated arguments to constructors work
        assert new B("hello", " world!").toString().equals("hello world!");
        check(() -> new B(null, " world!").toString(), null, "assertion failed: a is null");
        check(() -> new B("hello", null).toString(), null, "assertion failed: b is null");
    }

    private String unannotatedArgument(String arg) {
        System.out.println("oneArgument: " + arg);
        return arg;
    }

    private String oneNotNullAnnotatedArgument(@NotNull String arg) {
        System.out.println("oneNotNullAnnotatedArgument: " + arg);
        return arg;
    }

    private String twoNotNullAnnotatedArguments(@NotNull String arg1, @NotNull String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("twoNotNullAnnotatedArguments: " + s);
        return s;
    }

    // @NotNull

    private String firstArgumentNotNullAnnotated(@NotNull String arg1, String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("firstArgumentNotNullAnnotated: " + s);
        return s;
    }

    private String secondArgumentNotNullAnnotated(String arg1, @NotNull String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("secondArgumentNotNullAnnotated: " + s);
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

    private String intermixedWithPrimitives(int a, @NotNull String txt, int b) {
        String s = String.format("%d%s%d", a, txt, b);
        System.out.println("intermixWithPrimitives: " + s);
        return s;
    }

    private <T> String genericParameter(@NotNull T arg) {
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

        if (!Objects.equals(assertionMessage, expectedExceptionMesssage)) {
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

    public record NotNullRecord(@NotNull String a, @NotNull String b) {}

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

        B(@NotNull String a, @NotNull String b) {
            super(a);
            this.b = b;
        }

        public String toString() {
            return super.toString() + b;
        }
    }

    class C<T> {
        private T t;

        C(@NotNull T t) {
            this.t = t;
        }

        @Override
        public String toString() {
            return String.valueOf(t);
        }

    }

    public String genericArguments(@NotNull String prefix, @NotNull String suffix, @NotNull Function<C<? extends Object>, String> func) {
        return prefix + func.apply(new C(suffix));
    }

}
