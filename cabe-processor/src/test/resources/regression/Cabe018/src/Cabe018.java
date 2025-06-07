/*
 * Testing Cabe#18: {@code @Nullable} annotation is ignored.
 *
 * Test succeeds if no exception is thrown when class is loaded and method called successfully.
 */
public class Cabe018 {
    public static void main(String[] args) {
        try {
            NullableIgnoredInEnumConstructor.main(new String[] {});
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("FAIL");
            System.exit(1);
        }
    }
}
