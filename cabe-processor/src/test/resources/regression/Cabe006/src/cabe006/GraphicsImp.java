package cabe6;

public class GraphicsImp implements Graphics {

    @Override
    public void drawText(CharSequence text, float x, float y) {
        drawText(text, x, y, HAnchor.LEFT, VAnchor.TOP);
    }

    @Override
    public void close() {}
}