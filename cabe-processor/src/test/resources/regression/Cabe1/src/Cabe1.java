/*
 * Testing Cabe#1: Adding assertions to records results in invalid class files
 *
 * Test succeeds if no exception is thrown when class is loaded and method called successfully.
 */
public class Cabe1 {
    public static void main(String[] args) {
        try {
            InvalidClassFile_Record instance = new InvalidClassFile_Record(InvalidClassFile_Record.class.getName());
            instance.arg();
            System.out.println("OK");
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("FAIL");
            System.exit(1);
        }
    }
}
