import org.jspecify.annotations.NullMarked;

@SuppressWarnings("java:S1220")
@NullMarked
public class SomeClass extends AbstractClass implements SomeInterface {
    @Override
    public void foo(Object arg) {
        /* nop */
    }

    @Override
    public void bar(Object arg) {
        /* nop */
    }
}
