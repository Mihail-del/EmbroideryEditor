package editor;

import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class MainController {

    @FXML
    private StackPane mainApplicationLayout;

    @FXML
    private VBox loadingScreen;

    @FXML
    private Label horizontalSymmetryBtn;

    @FXML
    private Label verticalSymmetryBtn;

    @FXML
    private Label fullPatternSymmetryBtn;

    private final Color IDLE_COLOR = Color.web("#252524"); // -fx-bg-secondary
    private final Color HOVER_COLOR = Color.web("#1f1f1e"); // -fx-bg-primary

    @FXML
    public void initialize() {
        simulateLoading();
        setupSymmetryButtonAnimations(horizontalSymmetryBtn);
        setupSymmetryButtonAnimations(verticalSymmetryBtn);
        setupSymmetryButtonAnimations(fullPatternSymmetryBtn);
    }

    private void setupSymmetryButtonAnimations(Label label) {
        label.setOnMouseEntered(e -> animateBackground(label, IDLE_COLOR, HOVER_COLOR, true));
        label.setOnMouseExited(e -> animateBackground(label, HOVER_COLOR, IDLE_COLOR, false));
    }

    private void animateBackground(Label label, Color from, Color to, boolean isHover) {
        Transition transition = new Transition() {
            {
                setCycleDuration(Duration.millis(300));
            }
            @Override
            protected void interpolate(double frac) {
                Color interpolated = from.interpolate(to, frac);
                String colorHex = String.format("#%02X%02X%02X",
                        (int) (interpolated.getRed() * 255),
                        (int) (interpolated.getGreen() * 255),
                        (int) (interpolated.getBlue() * 255));

                String style = "-fx-background-color: " + colorHex + ";";
                if (isHover) {
                    style += "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 10, 0, 0, 2);";
                }
                label.setStyle(style);
            }
        };
        transition.play();
    }

    private void simulateLoading() {
        PauseTransition delay = new PauseTransition(Duration.seconds(0));
        delay.setOnFinished(event -> {
            mainApplicationLayout.getChildren().remove(loadingScreen);
        });
        delay.play();
    }
}
