package cabe6;

/*
 * Testing Cabe#6: Instrumentation error / method not found
 */
public class Cabe006 {
    public static void main(String[] args) {
        GraphicsImp g = new GraphicsImp();
        g.drawText("test", 1, 2, Graphics.HAnchor.LEFT, Graphics.VAnchor.TOP);

        try {
            g.drawText("test", 1, 2, null, Graphics.VAnchor.TOP);
            System.out.println("should throw an exception");
            return;
        } catch (NullPointerException|AssertionError e) {
            // ignore
        }

        try {
            g.drawText("test", 1, 2, Graphics.HAnchor.LEFT, null);
            System.out.println("should throw an exception");
            return;
        } catch (NullPointerException|AssertionError e) {
            // ignore
        }

        System.out.println("OK");
    }
}
