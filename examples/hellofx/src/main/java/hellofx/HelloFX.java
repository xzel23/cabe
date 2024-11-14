package hellofx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;

@NullMarked
public class HelloFX extends Application {

    private static InputStream getResourceAsStream(String name) {
        return HelloFX.class.getResourceAsStream(name);
    }

    private Image loadImage(String resourceName) throws IOException {
        try (InputStream in = getResourceAsStream(resourceName)) {
            return new Image(in);
        }
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load the image from the classpath
        Image image = loadImage("logo.png");

        // Create an ImageView to display the image
        ImageView imageView = new ImageView(image);

        // Add the ImageView to a layout pane
        StackPane root = new StackPane(imageView);

        // Create a Scene with the layout pane
        Scene scene = new Scene(root, 600, 400);

        // Setup the Stage
        primaryStage.setTitle("JavaFX Image Display");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
