/*
 * Testing Cabe#5: instrumentation fails on abstract methods
 */
public class Cabe005 {
    public static void main(String[] args) {
        SomeClass instance = new SomeClass();

        instance.foo("test");

        try {
            instance.foo(null);
            System.out.println("foo(null) should throw");
            return;
        } catch (NullPointerException|AssertionError e) {
            // ignore
        }

        instance.bar("test");

        try {
            instance.bar(null);
            System.out.println("bar(null) should throw");
            return;
        } catch (NullPointerException|AssertionError e) {
            // ignore
        }

        System.out.println("OK");
    }
}
