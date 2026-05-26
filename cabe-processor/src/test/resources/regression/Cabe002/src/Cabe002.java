/*
 * Testing Cabe#1: Adding assertions to records results in invalid class files
 *
 * Test succeeds if no exception is thrown when class is loaded and method called successfully.
 */
@SuppressWarnings("java:S1220")
public class Cabe002 {
    public static void main(String[] args) {
        try {
            NullCheckGeneratedForImplicitRecordEquals a = new NullCheckGeneratedForImplicitRecordEquals("a");
            NullCheckGeneratedForImplicitRecordEquals b = new NullCheckGeneratedForImplicitRecordEquals("b");
            if (!a.equals(a)) {
                System.err.println("FAIL - A.equals(A) should return true");
                System.exit(1);
            }
            if (a.equals(b)) {
                System.err.println("FAIL - A.equals(B) should return false");
                System.exit(1);
            }
            if (a.equals(null)) {
                System.err.println("FAIL - A.equals(null) should return false");
                System.exit(1);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("FAIL");
            System.exit(1);
        }

        try {
            NullCheckGeneratedForImplicitNullMarkedRecordEquals A = new NullCheckGeneratedForImplicitNullMarkedRecordEquals("a");
            NullCheckGeneratedForImplicitNullMarkedRecordEquals B = new NullCheckGeneratedForImplicitNullMarkedRecordEquals("b");
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
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("FAIL");
            System.exit(1);
        }

        System.out.println("OK");
    }
}
