/*
 * Testing Cabe#1: Adding assertions to records results in invalid class files
 *
 * Test succeeds if no exception is thrown when class is loaded and method called successfully.
 */
public class Cabe2 {
    public static void main(String[] args) {
        try {
            NullCheckGeneratedForImplicitRecordEquals A = new NullCheckGeneratedForImplicitRecordEquals("A");
            NullCheckGeneratedForImplicitRecordEquals B = new NullCheckGeneratedForImplicitRecordEquals("B");
            if (!A.equals(A)) {
                System.err.println("FAIL - A.equals(A) should return true");
                System.exit(1);
            }
            if (A.equals(B)) {
                System.err.println("FAIL - A.equals(B) should return false");
                System.exit(1);
            }
            if (A.equals(null)) {
                System.err.println("FAIL - A.equals(null) should return false");
                System.exit(1);
            }
            System.out.println("OK");
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("FAIL");
            System.exit(1);
        }
    }
}
