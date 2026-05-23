package editor;

import javafx.animation.Transition;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Manages the thread color palette: color circle slots, the color picker,
 * hover animations, and active color selection.
 *
 * <p>This class follows the Single Responsibility Principle by encapsulating
 * all palette UI logic separately from the main controller.</p>
 */
public class ThreadPaletteManager {

    private static final String CIRCLE_COLOR_KEY = "circleBaseColor";
    private static final String CIRCLE_EMPTY_KEY = "circleIsEmpty";
    private static final String ACTIVE_CIRCLE_CLASS = "color-circle-active";

    private StackPane activeColorCircle;
    private ColorPicker threadColorPicker;
    private Color currentThreadColor = Color.web("#D97757");

    private final StackPane mainApplicationLayout;
    private Runnable hideAllMenusCallback;

    /**
     * Creates a new ThreadPaletteManager.
     *
     * @param mainApplicationLayout the root layout pane where the color picker is added
     */
    public ThreadPaletteManager(StackPane mainApplicationLayout) {
        this.mainApplicationLayout = mainApplicationLayout;
    }

    /**
     * Sets the callback to invoke when menus should be hidden (before opening the picker).
     */
    public void setHideAllMenusCallback(Runnable callback) {
        this.hideAllMenusCallback = callback;
    }

    /**
     * Returns the currently selected thread color.
     */
    public Color getCurrentThreadColor() {
        return currentThreadColor;
    }

    /**
     * Returns the internal ColorPicker instance (needed by MenuManager for hiding).
     */
    public ColorPicker getColorPicker() {
        return threadColorPicker;
    }

    // ── Initialization ───────────────────────────────────────────────

    /**
     * Creates and adds the hidden color picker to the main layout.
     * Must be called during controller initialization.
     */
    public void initColorPicker() {
        if (mainApplicationLayout == null) {
            return;
        }
        threadColorPicker = new ColorPicker();
        threadColorPicker.getStyleClass().add("thread-color-picker");
        threadColorPicker.setManaged(false);
        threadColorPicker.setVisible(true);
        threadColorPicker.setOpacity(0.0);
        threadColorPicker.setPrefSize(0, 0);
        mainApplicationLayout.getChildren().add(threadColorPicker);
        threadColorPicker.setOnAction(e -> {
            if (activeColorCircle == null) {
                return;
            }
            Color selected = threadColorPicker.getValue();
            if (selected != null) {
                applyCircleColor(activeColorCircle, selected);
            }
        });
    }

    /**
     * Sets up a color circle slot with its initial color and interaction handlers.
     *
     * @param pane      the StackPane representing the circle
     * @param baseColor the initial color (Color.TRANSPARENT for empty slots)
     */
    public void setupColorCircle(StackPane pane, Color baseColor) {
        if (pane == null) return;
        boolean isEmpty = baseColor.equals(Color.TRANSPARENT);
        setCircleState(pane, baseColor, isEmpty);

        pane.setOnMouseEntered(e -> animateCircleHover(pane, getCircleColor(pane), isCircleEmpty(pane), true));
        pane.setOnMouseExited(e -> animateCircleHover(pane, getCircleColor(pane), isCircleEmpty(pane), false));

        pane.setOnMouseClicked(e -> {
            if (isCircleEmpty(pane)) {
                // If it's an empty slot (+), any mouse click makes it active and shows the palette
                setActiveCircle(pane);
                openColorPicker(pane);
            } else {
                // If it already has a color
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    // Left click: Only make it active
                    setActiveCircle(pane);
                } else if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                    // Right click: Make it active AND open palette to replace the color
                    setActiveCircle(pane);
                    openColorPicker(pane);
                }
            }
        });
    }

    /**
     * Makes the given circle the active one and updates the current thread color.
     *
     * @param pane the circle to activate
     */
    public void setActiveCircle(StackPane pane) {
        if (pane == null) {
            return;
        }
        if (activeColorCircle != null) {
            activeColorCircle.getStyleClass().remove(ACTIVE_CIRCLE_CLASS);
        }
        activeColorCircle = pane;
        if (!activeColorCircle.getStyleClass().contains(ACTIVE_CIRCLE_CLASS)) {
            activeColorCircle.getStyleClass().add(ACTIVE_CIRCLE_CLASS);
        }
        Color color = getCircleColor(pane);
        if (color != null && !color.equals(Color.TRANSPARENT)) {
            currentThreadColor = color;
        }
    }

    // ── Color Picker ─────────────────────────────────────────────────

    private void openColorPicker(StackPane pane) {
        if (threadColorPicker == null || pane == null) {
            return;
        }
        if (hideAllMenusCallback != null) {
            hideAllMenusCallback.run();
        }
        activeColorCircle = pane;
        Color current = getCircleColor(pane);
        if (current == null || current.equals(Color.TRANSPARENT)) {
            current = Color.WHITE;
        }
        threadColorPicker.setValue(current);
        threadColorPicker.show();
    }

    private void applyCircleColor(StackPane pane, Color color) {
        setCircleState(pane, color, false);
        pane.setStyle("-fx-background-color: " + toRgb(color) + ";");
        pane.getChildren().removeIf(node -> node instanceof Label && "+".equals(((Label) node).getText()));
        currentThreadColor = color;
    }

    // ── Circle State ─────────────────────────────────────────────────

    private void setCircleState(StackPane pane, Color color, boolean isEmpty) {
        pane.getProperties().put(CIRCLE_COLOR_KEY, color);
        pane.getProperties().put(CIRCLE_EMPTY_KEY, isEmpty);
        if (isEmpty) {
            if (!pane.getStyleClass().contains("color-empty")) {
                pane.getStyleClass().add("color-empty");
            }
        } else {
            pane.getStyleClass().remove("color-empty");
        }
    }

    private Color getCircleColor(StackPane pane) {
        Object color = pane.getProperties().get(CIRCLE_COLOR_KEY);
        return color instanceof Color ? (Color) color : Color.TRANSPARENT;
    }

    private boolean isCircleEmpty(StackPane pane) {
        Object empty = pane.getProperties().get(CIRCLE_EMPTY_KEY);
        return empty instanceof Boolean ? (Boolean) empty : true;
    }

    // ── Animations ───────────────────────────────────────────────────

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
                }
            }
        };
        transition.play();
    }

    // ── Utilities ────────────────────────────────────────────────────

    private String toRgb(Color color) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }
}
