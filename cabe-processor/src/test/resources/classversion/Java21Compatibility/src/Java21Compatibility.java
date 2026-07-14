import org.jspecify.annotations.NullMarked;

@NullMarked
public class Java21Compatibility {
    public static void main(String[] args) {
        Java21Compatibility test = new Java21Compatibility();
        expectNpe(() -> test.publicApi(null), "value is null");
        expectAssertion(() -> test.privateApi(null), "secret is null");
        System.out.print("OK");
    }

    public String publicApi(String value) {
        return value;
    }

    private String privateApi(String secret) {
        return secret;
    }

    private static void expectNpe(Runnable task, String expectedMessage) {
        try {
            task.run();
            throw new IllegalStateException("expected NullPointerException");
        } catch (NullPointerException e) {
            if (!expectedMessage.equals(e.getMessage())) {
                throw new IllegalStateException("unexpected NullPointerException message: " + e.getMessage());
            }
        }
    }

    private static void expectAssertion(Runnable task, String expectedMessage) {
        try {
            task.run();
            throw new IllegalStateException("expected AssertionError");
        } catch (AssertionError e) {
            if (!expectedMessage.equals(e.getMessage())) {
                throw new IllegalStateException("unexpected AssertionError message: " + e.getMessage());
            }
        }
    }
}
