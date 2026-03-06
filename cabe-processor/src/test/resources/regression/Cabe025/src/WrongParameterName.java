import org.jspecify.annotations.*;
import java.util.function.Supplier;

@NullMarked
public class WrongParameterName {
    public void filterAndDispatch(long timestamp, String loggerName, Object lvl, @Nullable String mrk, @Nullable Object mdc, Object locationResolver, Supplier<CharSequence> msg, @Nullable Throwable t) {
    }
}
