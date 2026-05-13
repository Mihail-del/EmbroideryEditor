package ua.univercity;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class EmbroideryEditor extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // load FXML
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/main_layout.fxml")));

        Scene scene = new Scene(root, 1200, 800);

        // load CSS
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm());

        primaryStage.setTitle("Vyshyvanka Editor");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}