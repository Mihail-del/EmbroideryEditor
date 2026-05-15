package editor;

import javafx.animation.Transition;
import javafx.animation.PauseTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.util.Duration;

public class MainController {

    @FXML
    private StackPane mainApplicationLayout;

    @FXML
    private VBox loadingScreen;

    @FXML
    private ProgressBar loadingBar;

    private Label activeNavLabel;

    @FXML
    private Label saveNavLabel;

    @FXML
    private Label openNavLabel;

    @FXML
    private Label infoNavLabel;

    @FXML
    private Label horizontalSymmetryBtn;

    @FXML
    private Label verticalSymmetryBtn;

    @FXML
    private Label fullPatternSymmetryBtn;

    @FXML
    private Label gridMinusBtn;

    @FXML
    private Label gridPlusBtn;

    @FXML
    private StackPane colorCircle1;

    @FXML
    private StackPane colorCircle2;

    @FXML
    private StackPane colorCircle3;

    @FXML
    private StackPane colorCircle4;

    private static final Color NAV_IDLE_TEXT = Color.web("#F9F9F7");
    private static final Color NAV_ACTIVE_TEXT = Color.web("#D97757");
    private static final Color NAV_BORDER_ACTIVE = Color.web("#D97757");
    private static final Color NAV_BORDER_TRANSPARENT = Color.web("#D97757", 0.0);

    private static final String NAV_STATE_KEY = "navState";

    private enum NavState {
        IDLE,
        ACTIVE
    }

    private final Color IDLE_COLOR = Color.web("#252524"); // -fx-bg-secondary
    private final Color HOVER_COLOR = Color.web("#1f1f1e"); // -fx-bg-primary

    @FXML
    public void initialize() {
        simulateLoading();

        activeNavLabel = (Label) mainApplicationLayout.lookup(".active-nav");
        if (activeNavLabel != null) {
            setupNavHover(activeNavLabel, true);
        }
        setupNavHover(saveNavLabel, false);
        setupNavHover(openNavLabel, false);
        setupNavHover(infoNavLabel, false);

        setupSymmetryButtonAnimations(horizontalSymmetryBtn);
        setupSymmetryButtonAnimations(verticalSymmetryBtn);
        setupSymmetryButtonAnimations(fullPatternSymmetryBtn);

        setupSymmetryButtonAnimations(gridMinusBtn);
        setupSymmetryButtonAnimations(gridPlusBtn);

        setupColorCircleAnimations(colorCircle1, Color.web("#D97757"));
        setupColorCircleAnimations(colorCircle2, Color.web("#F4AAA9"));
        setupColorCircleAnimations(colorCircle3, Color.TRANSPARENT);
        setupColorCircleAnimations(colorCircle4, Color.TRANSPARENT);
    }

    private void setupNavHover(Label label, boolean isActive) {
        label.getProperties().put(NAV_STATE_KEY, isActive ? NavState.ACTIVE : NavState.IDLE);
        setNavStyle(label, isActive ? NAV_ACTIVE_TEXT : NAV_IDLE_TEXT,
                isActive ? NAV_BORDER_ACTIVE : NAV_BORDER_TRANSPARENT,
                isActive);

        label.setOnMouseEntered(e -> animateNavTo(label, NavState.ACTIVE));
        label.setOnMouseExited(e -> {
            NavState targetState = (label == activeNavLabel) ? NavState.ACTIVE : NavState.IDLE;
            animateNavTo(label, targetState);
        });
    }

    private void animateNavTo(Label label, NavState targetState) {
        NavState currentState = (NavState) label.getProperties().getOrDefault(NAV_STATE_KEY, NavState.IDLE);
        if (currentState == targetState) {
            return;
        }

        Color fromText = currentState == NavState.ACTIVE ? NAV_ACTIVE_TEXT : NAV_IDLE_TEXT;
        Color toText = targetState == NavState.ACTIVE ? NAV_ACTIVE_TEXT : NAV_IDLE_TEXT;
        Color fromBorder = currentState == NavState.ACTIVE ? NAV_BORDER_ACTIVE : NAV_BORDER_TRANSPARENT;
        Color toBorder = targetState == NavState.ACTIVE ? NAV_BORDER_ACTIVE : NAV_BORDER_TRANSPARENT;

        Transition transition = new Transition() {
            {
                setCycleDuration(Duration.millis(300));
            }

            @Override
            protected void interpolate(double frac) {
                Color text = fromText.interpolate(toText, frac);
                Color border = fromBorder.interpolate(toBorder, frac);
                setNavStyle(label, text, border, targetState == NavState.ACTIVE);
            }
        };

        label.getProperties().put(NAV_STATE_KEY, targetState);
        transition.play();
    }

    private void setNavStyle(Label label, Color text, Color border, boolean isActive) {
        String borderWidth = isActive ? "0 0 2 0" : "0";
        String style = "-fx-text-fill: " + toRgba(text) + ";" +
                "-fx-border-color: " + toRgba(border) + ";" +
                "-fx-border-width: " + borderWidth + ";";
        label.setStyle(style);
    }

    private String toRgba(Color color) {
        return String.format("rgba(%d, %d, %d, %.3f)",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255),
                color.getOpacity());
    }

    private void setupSymmetryButtonAnimations(Label label) {
        label.setOnMouseEntered(e -> animateBackground(label, IDLE_COLOR, HOVER_COLOR, true));
        label.setOnMouseExited(e -> animateBackground(label, HOVER_COLOR, IDLE_COLOR, false));
    }

    private void setupColorCircleAnimations(StackPane pane, Color baseColor) {
        if (pane == null) return;
        boolean isEmpty = baseColor.equals(Color.TRANSPARENT);

        pane.setOnMouseEntered(e -> animateCircleHover(pane, baseColor, isEmpty, true));
        pane.setOnMouseExited(e -> animateCircleHover(pane, baseColor, isEmpty, false));
    }

    private void animateCircleHover(StackPane pane, Color baseColor, boolean isEmpty, boolean isHover) {
        Transition transition = new Transition() {
            {
                setCycleDuration(Duration.millis(300));
            }

            @Override
            protected void interpolate(double frac) {
                if (isEmpty) {
                    // Just animate a white glow for empty slots
                    double shadowOpacity = isHover ? 0.3 * frac : 0.3 * (1.0 - frac);
                    if (shadowOpacity > 0.02) {
                        pane.setEffect(new DropShadow(10, 0, 0, new Color(1, 1, 1, shadowOpacity)));
                    } else {
                        pane.setEffect(null);
                    }
                } else {
                    // Animate colored glow for filled slots
                    double shadowOpacity = isHover ? (0.35 + 0.4 * frac) : (0.35 + 0.4 * (1.0 - frac));
                    double glowRadius = isHover ? (6 + 8 * frac) : (6 + 8 * (1.0 - frac));

                    Color shadowColor = new Color(
                        baseColor.getRed(),
                        baseColor.getGreen(),
                        baseColor.getBlue(),
                        shadowOpacity
                    );
                    pane.setEffect(new DropShadow(glowRadius, 0, 0, shadowColor));

                    // optional scale logic could go here
                }
            }
        };
        transition.play();
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
        if (loadingBar != null) {
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(loadingBar.progressProperty(), 0.0)),
                new KeyFrame(Duration.seconds(5), new KeyValue(loadingBar.progressProperty(), 1.0))
            );
            timeline.play();
        }

        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(event -> {
            mainApplicationLayout.getChildren().remove(loadingScreen);
        });
        delay.play();
    }
}
