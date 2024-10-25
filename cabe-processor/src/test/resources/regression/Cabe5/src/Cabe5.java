/*
 * Testing Cabe#1: Adding assertions to records results in invalid class files
 *
 * Test succeeds if no exception is thrown when class is loaded and method called successfully.
 */
public class Cabe5 {
    public static void main(String[] args) {
        try {
            SomeClass instance = new SomeClass();

            instance.foo("test");

            try {
                instance.foo(null);
                System.out.println("foo(null) should throw");
            } catch (NullPointerException|AssertionError e) {
                // ignore
            }

            instance.bar("test");

            try {
                instance.bar(null);
                System.out.println("bar(null) should throw");
            } catch (NullPointerException|AssertionError e) {
                // ignore
            }

            System.out.println("OK");
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("FAIL");
            System.exit(1);
        }
    }
}
