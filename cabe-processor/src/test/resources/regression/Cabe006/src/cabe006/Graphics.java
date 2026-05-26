package cabe006;

public interface Graphics extends AutoCloseable {

    void drawText(CharSequence text, float x, float y);

    enum HAnchor {
        LEFT,
        RIGHT,
        CENTER
    }

    enum VAnchor {
        TOP,
        BOTTOM,
        BASELINE,
        MIDDLE
    }

    default void drawText(CharSequence text, float x, float y, HAnchor hAnchor, VAnchor vAnchor) {
    }

    @Override
    void close();
}