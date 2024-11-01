package modular.nullmarked;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/*
 * Testing Cabe#4: @NullMarked annotation on module has no effect
 */
public class Cabe004 {

    public static void main(String[] args) {
        // check processing of unannotated arguments
        check(() -> unannotatedArgument("hello world!"), "hello world!", null);
        check(() -> unannotatedArgument(null), null, "assertion failed: (arg|arg#1) is null");

        // @NonNull
        check(() -> oneNonNullAnnotatedArgument("hello world!"), "hello world!", null);
        check(() -> oneNonNullAnnotatedArgument(null), null, "assertion failed: (arg|arg#1) is null");

        check(() -> twoNonNullAnnotatedArguments("hello", "world!"), "hello world!", null);
        check(() -> twoNonNullAnnotatedArguments(null, "world!"), null, "assertion failed: (arg1|arg#1) is null");
        check(() -> twoNonNullAnnotatedArguments("hello", null), null, "assertion failed: (arg2|arg#2) is null");
        check(() -> twoNonNullAnnotatedArguments(null, null), null, "assertion failed: (arg1|arg#1) is null");

        check(() -> firstArgumentNonNullAnnotated("hello", "world!"), "hello world!", null);
        check(() -> firstArgumentNonNullAnnotated(null, "world!"), null, "assertion failed: (arg1|arg#1) is null");
        check(() -> firstArgumentNonNullAnnotated("hello", null), null, "assertion failed: (arg2|arg#2) is null");
        check(() -> firstArgumentNonNullAnnotated(null, null), null, "assertion failed: (arg1|arg#1) is null");

        check(() -> secondArgumentNonNullAnnotated("hello", "world!"), "hello world!", null);
        check(() -> secondArgumentNonNullAnnotated(null, "world!"), null, "assertion failed: (arg1|arg#1) is null");
        check(() -> secondArgumentNonNullAnnotated("hello", null), null, "assertion failed: (arg2|arg#2) is null");
        check(() -> secondArgumentNonNullAnnotated(null, null), null, "assertion failed: (arg1|arg#1) is null");

        // @Nullable
        check(() -> oneNullableAnnotatedArgument("hello world!"), "hello world!", null);
        check(() -> oneNullableAnnotatedArgument(null), null, null);

        check(() -> twoNullableAnnotatedArguments("hello", "world!"), "hello world!", null);
        check(() -> twoNullableAnnotatedArguments(null, "world!"), "null world!", null);
        check(() -> twoNullableAnnotatedArguments("hello", null), "hello null", null);
        check(() -> twoNullableAnnotatedArguments(null, null), "null null", null);

        check(() -> firstArgumentNullableAnnotated("hello", "world!"), "hello world!", null);
        check(() -> firstArgumentNullableAnnotated(null, "world!"), "null world!", null);
        check(() -> firstArgumentNullableAnnotated("hello", null), null, "assertion failed: (arg2|arg#2) is null");
        check(() -> firstArgumentNullableAnnotated(null, null), null, "assertion failed: (arg2|arg#2) is null");

        check(() -> secondArgumentNullableAnnotated("hello", "world!"), "hello world!", null);
        check(() -> secondArgumentNullableAnnotated(null, "world!"), null, "assertion failed: (arg1|arg#1) is null");
        check(() -> secondArgumentNullableAnnotated("hello", null), "hello null", null);
        check(() -> secondArgumentNullableAnnotated(null, null), null, "assertion failed: (arg1|arg#1) is null");

        // record parameter
        check(() -> new Pair("A", 1).toString(), "Pair[first=A, second=1]", null);
        check(() -> new Pair(null, 1).toString(), null, "npe: first is null");
        check(() -> Pair.of("A", 1).toString(), "Pair[first=A, second=1]", null);
        check(() -> new NullablePair("A", 1).toString(), "NullablePair[first=A, second=1]", null);
        check(() -> new NullablePair("A", null).toString(), "NullablePair[first=A, second=null]", null);
        check(() -> new NullablePair(null, 1).toString(), null, "npe: first is null");

        // primitive argument
        check(() -> primitiveArgument(1), "hello 1", null);

        // check that annotated arguments to constructors work
        assert new B("hello", " world!").toString().equals("hello world!");

        // check that enum constructors work
        check(() -> F.WITH_INITIALISER_1.toString(), null, "assertion failed: (txt|arg#2) is null");

        // check that lambdas are not instrumented
        check(() -> apply(a -> String.valueOf(a), 1), "1", null);
        check(() -> apply(a -> String.valueOf(a), null), "null", null);

        System.out.println("OK");
    }

    private static String unannotatedArgument(String arg) {
        System.err.println("oneArgument: " + arg);
        return arg;
    }

    private static String oneNonNullAnnotatedArgument(@NonNull String arg) {
        System.err.println("oneNonNullAnnotatedArgument: " + arg);
        return arg;
    }

    // @NonNull

    private static String twoNonNullAnnotatedArguments(@NonNull String arg1, @NonNull String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.err.println("twoNonNullAnnotatedArguments: " + s);
        return s;
    }

    private static String firstArgumentNonNullAnnotated(@NonNull String arg1, String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.err.println("firstArgumentNonNullAnnotated: " + s);
        return s;
    }

    private static String secondArgumentNonNullAnnotated(String arg1, @NonNull String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.err.println("secondArgumentNonNullAnnotated: " + s);
        return s;
    }

    private static String oneNullableAnnotatedArgument(@Nullable String arg) {
        System.err.println("oneNullableAnnotatedArgument: " + arg);
        return arg;
    }

    // @Nullable

    private static String twoNullableAnnotatedArguments(@Nullable String arg1, @Nullable String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.err.println("twoNullableAnnotatedArguments: " + s);
        return s;
    }

    private static String firstArgumentNullableAnnotated(@Nullable String arg1, String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.err.println("firstArgumentNullableAnnotated: " + s);
        return s;
    }

    private static String secondArgumentNullableAnnotated(String arg1, @Nullable String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.err.println("secondArgumentNullableAnnotated: " + s);
        return s;
    }

    private static String primitiveArgument(int i) {
        return "hello " + i;
    }

    // primitive arguments

    private static void check(Supplier<String> task, @Nullable String expectedResult, @Nullable String expectedExceptionMesssage) {
        String assertionMessage = null;
        String result = null;
        try {
            result = task.get();
        } catch (AssertionError ae) {
            assertionMessage = "assertion failed: " + ae.getMessage();
        } catch (NullPointerException e) {
            assertionMessage = "npe: " + e.getMessage();
        }

        if (assertionMessage != expectedExceptionMesssage && !String.valueOf(assertionMessage).matches(String.valueOf(expectedExceptionMesssage))) {
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

    // helper methods

    public record Pair<T1, T2>(T1 first, T2 second) {
        public static <T1, T2> Pair<T1, T2> of(T1 first, T2 second) {
            return new Pair<>(first, second);
        }
    }

    private record NullablePair(Object first, @Nullable Object second) {
    }

    private static class A {
        private String s;

        A(String s) {
            this.s = s;
        }

        public String toString() {
            return s;
        }
    }

    private static class B extends A {
        private String b;

        B(@NonNull String a, @NonNull String b) {
            super(a);
            this.b = b;
        }

        public String toString() {
            return super.toString() + b;
        }
    }

    enum E {
        UNUSED_1,
        UNUSED_2
    }

    enum F {
        WITH_INITIALISER_1(1, "one"),
        WITH_INITIALISER_2(2, null);

        F(int i, @NonNull String txt) {
            // nop
        }
    }

    static String apply(Function<Object, String> f, @Nullable Object arg) {
        return f.apply(arg);
    }
}
