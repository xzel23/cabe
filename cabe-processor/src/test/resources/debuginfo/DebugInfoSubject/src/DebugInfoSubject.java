import org.jspecify.annotations.NullMarked;

@NullMarked
public class DebugInfoSubject {
    public String publicApi(String value) {
        String result = value.trim();
        return decorate(result);
    }

    private String decorate(String value) {
        String prefix = "value=";
        return prefix + value;
    }
}
