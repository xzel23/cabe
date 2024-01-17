package com.dua3.cabe.processor.test.instrument;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class NoAnnotations {

    public static void test() {
        new ParameterAnnotations().doTest();
    }

    public void doTest() {
        check(() -> oneNotNullArgument("hello world!"), "hello world!", null);
        check(() -> oneNotNullArgument(null), null, "assertion failed: arg is null");
    }

    private String oneNotNullArgument(String arg) {
        assert arg!=null : "arg is null";
        System.out.println("oneNotNullAnnotatedArgument: " + arg);
        return arg;
    }

    private void check(Supplier<String> task, String expectedResult, String expectedExceptionMesssage) {
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
}
