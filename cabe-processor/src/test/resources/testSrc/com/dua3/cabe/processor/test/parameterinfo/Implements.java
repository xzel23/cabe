package com.dua3.cabe.processor.test.parameterinfo;

public class Implements implements Interface{
    public static void test() {
        new Implements().runTests();
    }

    public void runTests() {
        testImplements();
    }

    public void testImplements() {
        new Implements();
        new Implements("arg1");
        new Implements("arg1", "arg2");
        new Implements("arg1", "arg2", "arg3");
        new Implements("arg1", new Object[]{"args"});

        Implements instance = new Implements();
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

        dim();
        dim("arg1");
        dim("arg1", "arg2");
        dim("arg1", "arg2", "arg3");
        dim("arg1", new Object[]{"args"});
    }

    public Implements() {
        System.out.format("%s()%n", getClass().getSimpleName());
    }

    public Implements(String arg1) {
        System.out.format("%s(String arg1)%n", getClass().getSimpleName());
    }

    public Implements(String arg1, String arg2) {
        System.out.format("%s(String arg1, String arg2)%n", getClass().getSimpleName());
    }

    public Implements(String arg1, String arg2, String arg3) {
        System.out.format("%s(String arg1, String arg2, String arg3)%n", getClass().getSimpleName());
    }

    public Implements(String arg1, Object... args) {
        System.out.format("%s(String arg1, Object... args)%n", getClass().getSimpleName());
    }

    @Override
    public void m() {
            System.out.format("%s.m()%n", getClass().getSimpleName());
        }

    @Override
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
        System.out.format("%s.m(String arg1, Object... args)%n", Implements.class.getSimpleName());
    }

    public static void sm() {
        System.out.format("%s.sm()%n", Implements.class.getSimpleName());
    }

    public static void sm(String arg1) {
        System.out.format("%s.sm(String arg1)%n", Implements.class.getSimpleName());
    }

    public static void sm(String arg1, String arg2) {
        System.out.format("%s.sm(String arg1, String arg2)%n", Implements.class.getSimpleName());
    }

    public static void sm(String arg1, String arg2, String arg3) {
        System.out.format("%s.sm(String arg1, String arg2, Stringg arg3)%n", Implements.class.getSimpleName());
    }

    public static void sm(String arg1, Object... args) {
        System.out.format("%s.sm(String arg1, Object... args)%n", Implements.class.getSimpleName());
    }
}
