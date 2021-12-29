package com.dua3.cabe.test.coba;

import java.util.Objects;
import com.dua3.cabe.annotations.NotNull;
import java.util.function.Supplier;

public class Coba {
    
    public static record Pair<T1,T2>(T1 first, T2 second) {
        public static <T1, T2> Pair<T1, T2> of(T1 first, T2 second) {
            return new Pair<>(first, second);
        }
    }
    
    public static void main(String[] args) {
        check(() -> oneNonNullAnnotatedArgument("hello world!"), "hello world!", null);
        check(() -> oneNonNullAnnotatedArgument(null), null, "error: parameter arg must not be null");
        
        check(() -> nonNullAnnotatedSecondArgument("hello", "world!"), "hello world!", null);
        check(() -> nonNullAnnotatedSecondArgument(null, "world!"), "null world!", null);
        check(() -> nonNullAnnotatedSecondArgument("hello", null), null, "error: parameter arg2 must not be null");
        check(() -> nonNullAnnotatedSecondArgument(null, null), null, "error: parameter arg2 must not be null");
        
        check(() -> twoNonNullAnnotatedArgument("hello", "world!"), "hello world!", null);
        check(() -> twoNonNullAnnotatedArgument(null, "world!"), null, "error: parameter arg1 must not be null");
        check(() -> twoNonNullAnnotatedArgument("hello", null), null, "error: parameter arg2 must not be null");
        check(() -> twoNonNullAnnotatedArgument(null, null), null, "error: parameter arg2 must not be null");

        check(() -> secondArgumentNonNullAnnotated("hello", "world!"), "hello world!", null);
        check(() -> secondArgumentNonNullAnnotated(null, "world!"), "null world!", null);
        check(() -> secondArgumentNonNullAnnotated("hello", null), null, "error: parameter arg2 must not be null");
        check(() -> secondArgumentNonNullAnnotated(null, null), null, "error: parameter arg2 must not be null");

        check(() -> firstArgumentNonNullAnnotated("hello", "world!"), "hello world!", null);
        check(() -> firstArgumentNonNullAnnotated(null, "world!"), null, "error: parameter arg1 must not be null");
        check(() -> firstArgumentNonNullAnnotated("hello", null), "hello null", null);
        check(() -> firstArgumentNonNullAnnotated(null, null), null, "error: parameter arg1 must not be null");
        
        check(() -> Pair.of("A", 1).toString(), "Pair[first=A, second=1]", null);
    }
    
    private static String oneNonNullAnnotatedArgument(@NotNull String arg) {
        System.out.println("oneNonNullAnnotatedArgument: "+arg);
        return arg;
    }

    private static String nonNullAnnotatedSecondArgument(String arg1, @NotNull String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("nonNullAnnotatedSecondArgument: "+s);
        return s;
    }
 
    private static String twoNonNullAnnotatedArgument(@NotNull String arg1, @NotNull String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("twoNonNullAnnotatedArgument: "+s);
        return s;
    }

    private static String secondArgumentNonNullAnnotated(String arg1, @NotNull String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("secondArgumentNonNullAnnotated: "+s);
        return s;
    }

    private static String firstArgumentNonNullAnnotated(@NotNull String arg1, String arg2) {
        String s = String.format("%s %s", arg1, arg2);
        System.out.println("firstArgumentNonNullAnnotated: "+s);
        return s;
    }

    private static void check(Supplier<String> task, String expectedResult, String expectedExceptionMesssage) {
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
    
}
