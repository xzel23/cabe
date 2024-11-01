package cabe6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

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