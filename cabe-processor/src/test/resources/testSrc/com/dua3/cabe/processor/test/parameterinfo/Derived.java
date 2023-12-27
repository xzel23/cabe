package com.dua3.cabe.processor.test.parameterinfo;

public class Derived extends Base {
    public static void test() {
        new Derived().runTests();
    }

    public void runTests() {
        testDerived();
    }

    public void testDerived() {
        new Derived();
        new Derived("arg1");
        new Derived("arg1", "arg2");
        new Derived("arg1", "arg2", "arg3");
        new Derived("arg1", new Object[]{"args"});

        Derived instance = new Derived();
        instance.m();
        instance.m("arg1");
        instance.m("arg1", "arg2");
        instance.m("arg1", "arg2", "arg3");
        instance.m("arg1", new Object[]{"args"});

        sm();
        sm("arg1");
        sm("arg1", "arg2");
        sm("arg1", "arg2", "arg3");
        sm("arg1", new Object[]{"args"});
    }

    public Derived() {
        System.out.format("%s()%n", getClass().getSimpleName());
    }

    public Derived(String arg1) {
        System.out.format("%s(String arg1)%n", getClass().getSimpleName());
    }

    public Derived(String arg1, String arg2) {
        System.out.format("%s(String arg1, String arg2)%n", getClass().getSimpleName());
    }

    public Derived(String arg1, String arg2, String arg3) {
        System.out.format("%s(String arg1, String arg2, String arg3)%n", getClass().getSimpleName());
    }

    public Derived(String arg1, Object... args) {
        System.out.format("%s(String arg1, Object... args)%n", getClass().getSimpleName());
    }

    @Override
    public void m() {
        System.out.format("%s.m()%n", getClass().getSimpleName());
    }

    public void m(String arg1) {
        System.out.format("%s.m(String arg1)%n", getClass().getSimpleName());
    }

    @Override
    public void m(String arg1, String arg2) {
        System.out.format("%s.m(String arg1, String arg2)%n", getClass().getSimpleName());
    }

    @Override
    public void m(String arg1, String arg2, String arg3) {
        System.out.format("%s.m(String arg1, String arg2, Stringg arg3)%n", getClass().getSimpleName());
    }

    @Override
    public void m(String arg1, Object... args) {
        System.out.format("%s.m(String arg1, Object... args)%n", getClass().getSimpleName());
    }

    public static void sm() {
        System.out.format("%s.sm()%n", Derived.class.getSimpleName());
    }

    public static void sm(String arg1) {
        System.out.format("%s.sm(String arg1)%n", Derived.class.getSimpleName());
    }

    public static void sm(String arg1, String arg2) {
        System.out.format("%s.sm(String arg1, String arg2)%n", Derived.class.getSimpleName());
    }

    public static void sm(String arg1, String arg2, String arg3) {
        System.out.format("%s.sm(String arg1, String arg2, Stringg arg3)%n", Derived.class.getSimpleName());
    }

    public static void sm(String arg1, Object... args) {
        System.out.format("%s.sm(String arg1, Object... args)%n", Derived.class.getSimpleName());
    }
}
