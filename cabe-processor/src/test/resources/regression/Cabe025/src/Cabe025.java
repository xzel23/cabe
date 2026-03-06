import org.jspecify.annotations.*;
import java.util.Objects;

/*
 * Testing Cabe#25: Parameter name not determined correctly
 *
 * Test succeeds if exception is thrown and names the correct parameter name.
 */
@NullMarked
public class Cabe025 {
    public static void main(String[] args) {
        WrongParameterName instance = new WrongParameterName();
        // should pass
        instance.filterAndDispatch(123L, "loggerName", 1, "mark", "mdc", "locationResolver", "msg"::toString, null);
        // should throw exception about loggerName being null
        try {
            instance.filterAndDispatch(123L, null, 1, "mark", "mdc", "locationResolver", "msg"::toString, null);
            System.out.println("FAIL - invalid null parameter not detected");
        } catch (Throwable t) {
            String message = t.getMessage();
            String expected = "loggerName is null";
            if (!Objects.equals(expected, message)) {
                System.out.println("FAILED");
                System.out.println("expected: " + expected);
                System.out.println("actual:   " + message);
            } else{
                System.out.println("OK");
            }
        }
    }
}
