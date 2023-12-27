package com.dua3.cabe.processor.test.parameterinfo;

public interface Interface {
    public static void test() {
        sim();
        sim("arg1");
        sim("arg1", "arg2");
        sim("arg1", "arg2", "arg3");
        sim("arg1", new Object[]{"args"});
    }

    void m();

    void m(String arg1);

    void m(String arg1, String arg2);

    void m(String arg1, String arg2, String arg3);

    void m(String arg1, Object... args);

    default void dim() {
        System.out.format("%s.m()%n", getClass().getSimpleName());
    }

    default void dim(String arg1) {
        System.out.format("%s.m(String arg1)%n", getClass().getSimpleName());
    }

    default void dim(String arg1, String arg2) {
        System.out.format("%s.m(String arg1, String arg2)%n", getClass().getSimpleName());
    }

    default void dim(String arg1, String arg2, String arg3) {
        System.out.format("%s.m(String arg1, String arg2, Stringg arg3)%n", getClass().getSimpleName());
    }

    default void dim(String arg1, Object... args) {
        System.out.format("%s.m(String arg1, Object... args)%n", getClass().getSimpleName());
    }

    static void sim () {
        System.out.format("%s.m()%n", Interface.class.getSimpleName());
    }

    static void sim (String arg1) {
        System.out.format("%s.m(String arg1)%n", Interface.class.getSimpleName());
    }

    static void sim (String arg1, String arg2) {
        System.out.format("%s.m(String arg1, String arg2)%n", Interface.class.getSimpleName());
    }

    static void sim (String arg1, String arg2, String arg3) {
        System.out.format("%s.m(String arg1, String arg2, Stringg arg3)%n", Interface.class.getSimpleName());
    }

    static void sim (String arg1, Object... args) {
        System.out.format("%s.m(String arg1, Object... args)%n", Interface.class.getSimpleName());
    }

    default void dimInnerclass() {
        new Interface() {
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
                System.out.format("%s.m(String arg1, Object... args)%n", getClass().getSimpleName());
            }
        };
    }
}
