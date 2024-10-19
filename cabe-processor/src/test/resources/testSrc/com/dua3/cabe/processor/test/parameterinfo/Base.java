package com.dua3.cabe.processor.test.parameterinfo;

import org.jspecify.annotations.Nullable;

public class Base {
    public static void test() {
        new Base().runTests();
    }

    public void runTests() {
        testBase();
        testStaticInner();
        testInner();
        testDerivedStaticInner();
        testDerivedInner();
        testInnerClasses();
        testInnerClassesDerived();
        testInnerClassesImplements();
    }

    public void testBase() {
        new Base();
        new Base("arg1");
        new Base("arg1", "arg2");
        new Base("arg1", "arg2", "arg3");
        new Base("arg1", new Object[]{"args"});

        Base instance = new Base();
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

        smAnonymousInnerClass("arg1", "arg2");
    }

    public void testStaticInner() {
        new StaticInner();
        new StaticInner("arg1");
        new StaticInner("arg1", "arg2");
        new StaticInner("arg1", "arg2", "arg3");
        new StaticInner("arg1", new Object[]{"args"});

        StaticInner instance = new StaticInner();
        instance.m();
        instance.m("arg1");
        instance.m("arg1", "arg2");
        instance.m("arg1", "arg2", "arg3");
        instance.m("arg1", new Object[]{"args"});

        StaticInner.sm();
        StaticInner.sm("arg1");
        StaticInner.sm("arg1", "arg2");
        StaticInner.sm("arg1", "arg2", "arg3");
        StaticInner.sm("arg1", new Object[]{"args"});
    }

    public void testInner() {
        new Inner();
        new Inner("arg1");
        new Inner("arg1", "arg2");
        new Inner("arg1", "arg2", "arg3");
        new Inner("arg1", new Object[]{"args"});

        Inner instance = new Inner();
        instance.m();
        instance.m("arg1");
        instance.m("arg1", "arg2");
        instance.m("arg1", "arg2", "arg3");
        instance.m("arg1", new Object[]{"args"});

        Inner.sm();
        Inner.sm("arg1");
        Inner.sm("arg1", "arg2");
        Inner.sm("arg1", "arg2", "arg3");
        Inner.sm("arg1", new Object[]{"args"});
    }

    public void testDerivedStaticInner() {
        new DerivedStaticInner();
        new DerivedStaticInner("arg1");
        new DerivedStaticInner("arg1", "arg2");
        new DerivedStaticInner("arg1", "arg2", "arg3");
        new DerivedStaticInner("arg1", new Object[]{"args"});

        DerivedStaticInner instance = new DerivedStaticInner();
        instance.m();
        instance.m("arg1");
        instance.m("arg1", "arg2");
        instance.m("arg1", "arg2", "arg3");
        instance.m("arg1", new Object[]{"args"});

        DerivedStaticInner.sm();
        DerivedStaticInner.sm("arg1");
        DerivedStaticInner.sm("arg1", "arg2");
        DerivedStaticInner.sm("arg1", "arg2", "arg3");
        DerivedStaticInner.sm("arg1", new Object[]{"args"});
    }

    public void testDerivedInner() {
        new DerivedInner();
        new DerivedInner("arg1");
        new DerivedInner("arg1", "arg2");
        new DerivedInner("arg1", "arg2", "arg3");
        new DerivedInner("arg1", new Object[]{"args"});

        DerivedInner instance = new DerivedInner();
        instance.m();
        instance.m("arg1");
        instance.m("arg1", "arg2");
        instance.m("arg1", "arg2", "arg3");
        instance.m("arg1", new Object[]{"args"});

        DerivedInner.sm();
        DerivedInner.sm("arg1");
        DerivedInner.sm("arg1", "arg2");
        DerivedInner.sm("arg1", "arg2", "arg3");
        DerivedInner.sm("arg1", new Object[]{"args"});
    }

    public Base() {
        System.out.format("%s()%n", getClass().getSimpleName());
    }

    public Base(String arg1) {
        System.out.format("%s(String arg1)%n", getClass().getSimpleName());
    }

    public Base(String arg1, String arg2) {
        System.out.format("%s(String arg1, String arg2)%n", getClass().getSimpleName());
    }

    public Base(String arg1, String arg2, String arg3) {
        System.out.format("%s(String arg1, String arg2, String arg3)%n", getClass().getSimpleName());
    }

    public Base(String arg1, Object... args) {
        System.out.format("%s(String arg1, Object... args)%n", getClass().getSimpleName());
    }

    public void m() {
        System.out.format("%s.m()%n", getClass().getSimpleName());
    }

    public void m(String arg1) {
        System.out.format("%s.m(String arg1)%n", getClass().getSimpleName());
    }

    public void m(String arg1, String arg2) {
        System.out.format("%s.m(String arg1, String arg2)%n", getClass().getSimpleName());
    }

    public void m(String arg1, String arg2, String arg3) {
        System.out.format("%s.m(String arg1, String arg2, String arg3)%n", getClass().getSimpleName());
    }

    public void m(String arg1, Object... args) {
        System.out.format("%s.m(String arg1, Object... args)%n", getClass().getSimpleName());
    }

    public static void sm() {
        System.out.format("%s.sm()%n", Base.class.getSimpleName());
    }

    public static void sm(String arg1) {
        System.out.format("%s.sm(String arg1)%n", Base.class.getSimpleName());
    }

    public static void sm(String arg1, String arg2) {
        System.out.format("%s.sm(String arg1, String arg2)%n", Base.class.getSimpleName());
    }

    public static void sm(String arg1, String arg2, String arg3) {
        System.out.format("%s.sm(String arg1, String arg2, Stringg arg3)%n", Base.class.getSimpleName());
    }

    public static void sm(String arg1, Object... args) {
        System.out.format("%s.sm(String arg1, Object... args)%n", Base.class.getSimpleName());
    }

    public static void smAnonymousInnerClass(@Nullable String arg1, @Nullable String arg2) {
        System.out.format("%s.smAnonymousInnerClass(String arg1, String arg2)%n", Base.class.getSimpleName());
        new Runnable() {
            {
                System.out.format("%s.sm()%n", getClass().getSimpleName());
            }
            public void run() {
                System.out.format("%s.run() - arg1 = '%s', arg2 = '%s'%n", getClass().getSimpleName(), arg1, arg2);
            }
        }.run();
    }

    public static class StaticInner {
        public StaticInner() {
            System.out.format("%s()%n", getClass().getSimpleName());
        }

        public StaticInner(String arg1) {
            System.out.format("%s(String arg1)%n", getClass().getSimpleName());
        }

        public StaticInner(String arg1, String arg2) {
            System.out.format("%s(String arg1, String arg2)%n", getClass().getSimpleName());
        }

        public StaticInner(String arg1, String arg2, String arg3) {
            System.out.format("%s(String arg1, String arg2, String arg3)%n", getClass().getSimpleName());
        }

        public StaticInner(String arg1, Object... args) {
            System.out.format("%s(String arg1, Object... args)%n", getClass().getSimpleName());
        }

        public void m() {
            System.out.format("%s.m()%n", getClass().getSimpleName());
        }

        public void m(String arg1) {
            System.out.format("%s.m(String arg1)%n", getClass().getSimpleName());
        }

        public void m(String arg1, String arg2) {
            System.out.format("%s.m(String arg1, String arg2)%n", getClass().getSimpleName());
        }

        public void m(String arg1, String arg2, String arg3) {
            System.out.format("%s.m(String arg1, String arg2, Stringg arg3)%n", getClass().getSimpleName());
        }

        public void m(String arg1, Object... args) {
            System.out.format("%s.m(String arg1, Object... args)%n", getClass().getSimpleName());
        }

        public static void sm() {
            System.out.format("%s.sm()%n", StaticInner.class.getSimpleName());
        }

        public static void sm(String arg1) {
            System.out.format("%s.sm(String arg1)%n", StaticInner.class.getSimpleName());
        }

        public static void sm(String arg1, String arg2) {
            System.out.format("%s.sm(String arg1, String arg2)%n", StaticInner.class.getSimpleName());
        }

        public static void sm(String arg1, String arg2, String arg3) {
            System.out.format("%s.sm(String arg1, String arg2, Stringg arg3)%n", StaticInner.class.getSimpleName());
        }

        public static void sm(String arg1, Object... args) {
            System.out.format("%s.sm(String arg1, Object... args)%n", StaticInner.class.getSimpleName());
        }
    }

    public class Inner {
        public Inner() {
            System.out.format("%s()%n", getClass().getSimpleName());
        }

        public Inner(String arg1) {
            System.out.format("%s(String arg1)%n", getClass().getSimpleName());
        }

        public Inner(String arg1, String arg2) {
            System.out.format("%s(String arg1, String arg2)%n", getClass().getSimpleName());
        }

        public Inner(String arg1, String arg2, String arg3) {
            System.out.format("%s(String arg1, String arg2, String arg3)%n", getClass().getSimpleName());
        }

        public Inner(String arg1, Object... args) {
            System.out.format("%s(String arg1, Object... args)%n", getClass().getSimpleName());
        }

        public void m() {
            System.out.format("%s.m()%n", getClass().getSimpleName());
        }

        public void m(String arg1) {
            System.out.format("%s.m(String arg1)%n", getClass().getSimpleName());
        }

        public void m(String arg1, String arg2) {
            System.out.format("%s.m(String arg1, String arg2)%n", getClass().getSimpleName());
        }

        public void m(String arg1, String arg2, String arg3) {
            System.out.format("%s.m(String arg1, String arg2, Stringg arg3)%n", getClass().getSimpleName());
        }

        public void m(String arg1, Object... args) {
            System.out.format("%s.m(String arg1, Object... args)%n", getClass().getSimpleName());
        }

        public static void sm() {
            System.out.format("%s.sm()%n", Inner.class.getSimpleName());
        }

        public static void sm(String arg1) {
            System.out.format("%s.sm(String arg1)%n", Inner.class.getSimpleName());
        }

        public static void sm(String arg1, String arg2) {
            System.out.format("%s.sm(String arg1, String arg2)%n", Inner.class.getSimpleName());
        }

        public static void sm(String arg1, String arg2, String arg3) {
            System.out.format("%s.sm(String arg1, String arg2, Stringg arg3)%n", Inner.class.getSimpleName());
        }

        public static void sm(String arg1, Object... args) {
            System.out.format("%s.sm(String arg1, Object... args)%n", Inner.class.getSimpleName());
        }
    }

    public static class DerivedStaticInner extends Base {
        public DerivedStaticInner() {
            System.out.format("%s()%n", getClass().getSimpleName());
        }

        public DerivedStaticInner(String arg1) {
            System.out.format("%s(String arg1)%n", getClass().getSimpleName());
        }

        public DerivedStaticInner(String arg1, String arg2) {
            System.out.format("%s(String arg1, String arg2)%n", getClass().getSimpleName());
        }

        public DerivedStaticInner(String arg1, String arg2, String arg3) {
            System.out.format("%s(String arg1, String arg2, String arg3)%n", getClass().getSimpleName());
        }

        public DerivedStaticInner(String arg1, Object... args) {
            System.out.format("%s(String arg1, Object... args)%n", getClass().getSimpleName());
        }

        public void m() {
            System.out.format("%s.m()%n", getClass().getSimpleName());
        }

        public void m(String arg1) {
            System.out.format("%s.m(String arg1)%n", getClass().getSimpleName());
        }

        public void m(String arg1, String arg2) {
            System.out.format("%s.m(String arg1, String arg2)%n", getClass().getSimpleName());
        }

        public void m(String arg1, String arg2, String arg3) {
            System.out.format("%s.m(String arg1, String arg2, Stringg arg3)%n", getClass().getSimpleName());
        }

        public void m(String arg1, Object... args) {
            System.out.format("%s.m(String arg1, Object... args)%n", getClass().getSimpleName());
        }

        public static void sm() {
            System.out.format("%s.sm()%n", DerivedStaticInner.class.getSimpleName());
        }

        public static void sm(String arg1, String arg2) {
            System.out.format("%s.sm(String arg1, String arg2)%n", DerivedStaticInner.class.getSimpleName());
        }

        public static void sm(String arg1, String arg2, String arg3) {
            System.out.format("%s.sm(String arg1, String arg2, Stringg arg3)%n", DerivedStaticInner.class.getSimpleName());
        }

        public static void sm(String arg1, Object... args) {
            System.out.format("%s.sm(String arg1, Object... args)%n", DerivedStaticInner.class.getSimpleName());
        }
    }


    public class DerivedInner extends Base {
        public DerivedInner() {
            System.out.format("%s()%n", getClass().getSimpleName());
        }

        public DerivedInner(String arg1) {
            System.out.format("%s(String arg1)%n", getClass().getSimpleName());
        }

        public DerivedInner(String arg1, String arg2) {
            System.out.format("%s(String arg1, String arg2)%n", getClass().getSimpleName());
        }

        public DerivedInner(String arg1, String arg2, String arg3) {
            System.out.format("%s(String arg1, String arg2, String arg3)%n", getClass().getSimpleName());
        }

        public DerivedInner(String arg1, Object... args) {
            System.out.format("%s(String arg1, Object... args)%n", getClass().getSimpleName());
        }

        public void m() {
            System.out.format("%s.m()%n", getClass().getSimpleName());
        }

        public void m(String arg1) {
            System.out.format("%s.m(String arg1)%n", getClass().getSimpleName());
        }

        public void m(String arg1, String arg2) {
            System.out.format("%s.m(String arg1, String arg2)%n", getClass().getSimpleName());
        }

        public void m(String arg1, String arg2, String arg3) {
            System.out.format("%s.m(String arg1, String arg2, Stringg arg3)%n", getClass().getSimpleName());
        }

        public void m(String arg1, Object... args) {
            System.out.format("%s.m(String arg1, Object... args)%n", getClass().getSimpleName());
        }

        public static void sm() {
            System.out.format("%s.sm()%n", DerivedInner.class.getSimpleName());
        }

        public static void sm(String arg1, String arg2) {
            System.out.format("%s.sm(String arg1, String arg2)%n", DerivedInner.class.getSimpleName());
        }

        public static void sm(String arg1, String arg2, String arg3) {
            System.out.format("%s.sm(String arg1, String arg2, Stringg arg3)%n", DerivedInner.class.getSimpleName());
        }

        public static void sm(String arg1, Object... args) {
            System.out.format("%s.sm(String arg1, Object... args)%n", DerivedInner.class.getSimpleName());
        }
    }

    public static void testInnerClasses() {
        var instance = new Object() {
            public void m() {
                System.out.format("%s.m()%n", getClass().getSimpleName());
            }

            public void m(String arg1) {
                System.out.format("%s.m(String arg1)%n", getClass().getSimpleName());
            }

            public void m(String arg1, String arg2) {
                System.out.format("%s.m(String arg1, String arg2)%n", getClass().getSimpleName());
            }

            public void m(String arg1, String arg2, String arg3) {
                System.out.format("%s.m(String arg1, String arg2, Stringg arg3)%n", getClass().getSimpleName());
            }

            public void m(String arg1, Object... args) {
                System.out.format("%s.m(String arg1, Object... args)%n", getClass().getSimpleName());
            }
        };

        instance.m();
        instance.m("arg1");
        instance.m("arg1", "arg2");
        instance.m("arg1", "arg2", "arg3");
        instance.m("arg1", new Object[]{"args"});
    }

    public static void testInnerClassesDerived() {
        var instance = new Base() {
            public void m() {
                System.out.format("%s.m()%n", getClass().getSimpleName());
            }

            public void m(String arg1, String arg2) {
                System.out.format("%s.m(String arg1, String arg2)%n", getClass().getSimpleName());
            }

            public void m(String arg1, String arg2, String arg3) {
                System.out.format("%s.m(String arg1, String arg2, Stringg arg3)%n", getClass().getSimpleName());
            }

            public void m(String arg1, Object... args) {
                System.out.format("%s.m(String arg1, Object... args)%n", getClass().getSimpleName());
            }
        };

        instance.m();
        instance.m("arg1");
        instance.m("arg1", "arg2");
        instance.m("arg1", "arg2", "arg3");
        instance.m("arg1", new Object[]{"args"});
    }

    public static void testInnerClassesImplements() {
        var instance = new Interface() {
            public void m() {
                System.out.format("%s.m()%n", getClass().getSimpleName());
            }

            public void m(String arg1) {
                System.out.format("%s.m(String arg1)%n", getClass().getSimpleName());
            }

            public void m(String arg1, String arg2) {
                System.out.format("%s.m(String arg1, String arg2)%n", getClass().getSimpleName());
            }

            public void m(String arg1, String arg2, String arg3) {
                System.out.format("%s.m(String arg1, String arg2, Stringg arg3)%n", getClass().getSimpleName());
            }

            public void m(String arg1, Object... args) {
                System.out.format("%s.m(String arg1, Object... args)%n", getClass().getSimpleName());
            }
        };

        instance.m();
        instance.m("arg1");
        instance.m("arg1", "arg2");
        instance.m("arg1", "arg2", "arg3");
        instance.m("arg1", new Object[]{"args"});
    }
}
