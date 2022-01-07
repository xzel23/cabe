package com.dua3.cabe.test.coba.api.notnull;

import java.util.Objects;
import com.dua3.cabe.annotations.*;
import java.util.function.Supplier;

public class NotNullPackage {
    
    public static record Pair<T1,T2>(T1 first, T2 second) {
        public static <T1, T2> Pair<T1, T2> of(T1 first, T2 second) {
            return new Pair<>(first, second);
        }
    }
    
    public static void test() {
        // check processing of unannotated arguments
        check(() -> unannotatedArgument("hello world!"), "hello world!", null);
        check(() -> unannotatedArgument(null), null, "error: parameter 'arg' must not be null");
        
        // @NotNull
        check(() -> oneNotNullAnnotatedArgument("hello world!"), "hello world!", null);
        check(() -> oneNotNullAnnotatedArgument(null), null, "error: parameter 'arg' must not be null");
        
        check(() -> twoNotNullAnnotatedArguments("hello", "world!"), "hello world!", null);
        check(() -> twoNotNullAnnotatedArguments(null, "world!"), null, "error: parameter 'arg1' must not be null");
        check(() -> twoNotNullAnnotatedArguments("hello", null), null, "error: parameter 'arg2' must not be null");
        check(() -> twoNotNullAnnotatedArguments(null, null), null, "error: parameter 'arg1' must not be null");

        check(() -> firstArgumentNotNullAnnotated("hello", "world!"), "hello world!", null);
        check(() -> firstArgumentNotNullAnnotated(null, "world!"), null, "error: parameter 'arg1' must not be null");
        check(() -> firstArgumentNotNullAnnotated("hello", null), null, "error: parameter 'arg2' must not be null");
        check(() -> firstArgumentNotNullAnnotated(null, null), null, "error: parameter 'arg1' must not be null");

        check(() -> secondArgumentNotNullAnnotated("hello", "world!"), "hello world!", null);
        check(() -> secondArgumentNotNullAnnotated(null, "world!"), null, "error: parameter 'arg1' must not be null");
        check(() -> secondArgumentNotNullAnnotated("hello", null), null, "error: parameter 'arg2' must not be null");
        check(() -> secondArgumentNotNullAnnotated(null, null), null, "error: parameter 'arg1' must not be null");
        
        // @Nullable
        check(() -> oneNullableAnnotatedArgument("hello world!"), "hello world!", null);
        check(() -> oneNullableAnnotatedArgument(null), null, null);

        check(() -> twoNullableAnnotatedArguments("hello", "world!"), "hello world!", null);
        check(() -> twoNullableAnnotatedArguments(null, "world!"), "null world!", null);
        check(() -> twoNullableAnnotatedArguments("hello", null), "hello null", null);
        check(() -> twoNullableAnnotatedArguments(null, null), "null null", null);

        check(() -> firstArgumentNullableAnnotated("hello", "world!"), "hello world!", null);
        check(() -> firstArgumentNullableAnnotated(null, "world!"), "null world!", null);
        check(() -> firstArgumentNullableAnnotated("hello", null), null, "error: parameter 'arg2' must not be null");
        check(() -> firstArgumentNullableAnnotated(null, null), null, "error: parameter 'arg2' must not be null");

        check(() -> secondArgumentNullableAnnotated("hello", "world!"), "hello world!", null);
        check(() -> secondArgumentNullableAnnotated(null, "world!"), null, "error: parameter 'arg1' must not be null");
        check(() -> secondArgumentNullableAnnotated("hello", null), "hello null", null);
        check(() -> secondArgumentNullableAnnotated(null, null), null, "error: parameter 'arg1' must not be null");

        // record parameter
        check(() -> Pair.of("A", 1).toString(), "Pair[first=A, second=1]", null);
        
        // primitive argument
        check(() -> primitiveArgument(1), "hello 1", null);
        
        // check that annotated arguments to constructors work
        assert new B("hello", " world!").toString().equals("hello world!");
    }

    private static String unannotatedArgument(String arg) {
        System.out.println("oneArgument: "+arg);
        return arg;
    }
    
    // @NotNull

    private static String oneNotNullAnnotatedArgument(@NotNull String arg) {
        System.out.println("oneNotNullAnnotatedArgument: "+arg);
        return arg;
    }

    private static String twoNotNullAnnotatedArguments(@NotNull String arg1, @NotNull String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("twoNotNullAnnotatedArguments: "+s);
        return s;
    }

    private static String firstArgumentNotNullAnnotated(@NotNull String arg1, String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("firstArgumentNotNullAnnotated: "+s);
        return s;
    }

    private static String secondArgumentNotNullAnnotated(String arg1, @NotNull String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("secondArgumentNotNullAnnotated: "+s);
        return s;
    }

    // @Nullable
    
    private static String oneNullableAnnotatedArgument(@Nullable String arg) {
        System.out.println("oneNullableAnnotatedArgument: "+arg);
        return arg;
    }

    private static String twoNullableAnnotatedArguments(@Nullable String arg1, @Nullable String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("twoNullableAnnotatedArguments: "+s);
        return s;
    }

    private static String firstArgumentNullableAnnotated(@Nullable String arg1, String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("firstArgumentNullableAnnotated: "+s);
        return s;
    }

    private static String secondArgumentNullableAnnotated(String arg1, @Nullable String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("secondArgumentNullableAnnotated: "+s);
        return s;
    }

    // primitive arguments
    
    private static String primitiveArgument(int i) {
        return "hello "+i;
    }
    
    // helper methods
    
    private static void check(Supplier<String> task, @Nullable String expectedResult, @Nullable String expectedExceptionMesssage) {
        String assertionMessage = null;
        String result = null;
        try {
            result = task.get();
        } catch (AssertionError ae) {
            assertionMessage = "error: "+ae.getMessage();
        }
        
        if (!Objects.equals(assertionMessage, expectedExceptionMesssage)) {
            System.err.format("expected exception: %s%nactual:   %s%n", expectedExceptionMesssage, assertionMessage);
            throw new IllegalStateException();
        }
        
        if (!Objects.equals(result, expectedResult)) {
            System.err.format("expected result:    %s%nactual:   %s%n", expectedResult, result);
            throw new IllegalStateException();
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
            return super.toString()+b;
        }
    }
}
