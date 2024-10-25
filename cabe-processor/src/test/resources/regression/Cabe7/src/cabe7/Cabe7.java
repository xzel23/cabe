package cabe7;

/*
 * Testing Cabe#7: Instrumentation error / constructor not found
 */
public class Cabe7 {
    public static void main(String[] args) {
        ObjectCache cache = new ObjectCache();

        cache.get(5);

        try {
            cache.get(null);
            System.out.println("FAIL");
        } catch (NullPointerException|AssertionError e) {
            //ignore
        }

        System.out.println("OK");
    }
}
