package editor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Objects;

public class EmbroideryEditor extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load custom font explicitly
        Font.loadFont(getClass().getResourceAsStream("/fonts/Serifa.ttf"), 14);

        // load FXML
        FXMLLoader fxmlLoader = new FXMLLoader(EmbroideryEditor.class.getResource("/main_layout.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);

        // load CSS
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm());

        primaryStage.setTitle("Vyshyvanka Editor");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}