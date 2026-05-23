package editor;

import javafx.animation.Transition;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Manages the top navigation bar labels: hover animations, active state tracking,
 * and animated button background effects for symmetry/grid controls.
 *
 * <p>This class follows the Single Responsibility Principle by isolating
 * navigation styling and animation logic from the main controller.</p>
 */
public class NavigationManager {

    private static final Color NAV_IDLE_TEXT = Color.web("#F9F9F7");
    private static final Color NAV_ACTIVE_TEXT = Color.web("#D97757");
    private static final Color NAV_BORDER_ACTIVE = Color.web("#D97757");
    private static final Color NAV_BORDER_TRANSPARENT = Color.web("#D97757", 0.0);

    private static final String NAV_STATE_KEY = "navState";

    private static final Color IDLE_COLOR = Color.web("#252524");
    private static final Color HOVER_COLOR = Color.web("#1f1f1e");

    private enum NavState {
        IDLE,
        ACTIVE
    }

    private Label activeNavLabel;

    /**
     * Sets the initially-active navigation label (looked up from CSS).
     *
     * @param activeLabel the label that starts in the active state
     */
    public void setActiveNavLabel(Label activeLabel) {
        this.activeNavLabel = activeLabel;
    }

    // ── Nav Hover ────────────────────────────────────────────────────

    /**
     * Configures hover enter/exit animations for a navigation label.
     *
     * @param label    the nav label
     * @param isActive true if this label should start in the active state
     */
    public void setupNavHover(Label label, boolean isActive) {
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
        String style = "-fx-text-fill: " + ProjectService.toRgba(text) + ";" +
                "-fx-border-color: " + ProjectService.toRgba(border) + ";" +
                "-fx-border-width: " + borderWidth + ";";
        label.setStyle(style);
    }

    // ── Button Background Animations ─────────────────────────────────

    /**
     * Adds hover enter/exit background color animation to a node
     * (used for symmetry buttons, grid +/- buttons, etc.).
     *
     * @param node the node to animate
     */
    public void setupButtonHoverAnimation(Node node) {
        node.setOnMouseEntered(e -> animateBackground(node, IDLE_COLOR, HOVER_COLOR, true));
        node.setOnMouseExited(e -> animateBackground(node, HOVER_COLOR, IDLE_COLOR, false));
    }

    private void animateBackground(Node node, Color from, Color to, boolean isHover) {
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
                node.setStyle(style);
            }
        };
        transition.play();
    }
}
