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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.util.Duration;
import javafx.beans.binding.Bindings;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ColorPicker;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import java.util.HashMap;
import java.util.Map;

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
    private StackPane canvasContainer;

    @FXML
    private StackPane mainCanvasView;

    @FXML
    private Canvas gridCanvas;

    @FXML
    private Canvas stitchCanvas;

    @FXML
    private HBox horizontalSymmetryBox;

    @FXML
    private HBox verticalSymmetryBox;

    @FXML
    private Label gridMinusBtn;

    @FXML
    private Label gridPlusBtn;

    @FXML
    private Label gridSizeLabel;

    @FXML
    private Button createGridBtn;

    @FXML
    private VBox createMenu;

    @FXML
    private TextField projectNameField;

    @FXML
    private StackPane colorCircle1;

    @FXML
    private StackPane colorCircle2;

    @FXML
    private StackPane colorCircle3;

    @FXML
    private StackPane colorCircle4;

    @FXML
    private StackPane colorCircle5;

    @FXML
    private StackPane colorCircle6;

    @FXML
    private StackPane colorCircle7;

    @FXML
    private StackPane colorCircle8;

    private static final String CIRCLE_COLOR_KEY = "circleBaseColor";
    private static final String CIRCLE_EMPTY_KEY = "circleIsEmpty";
    private static final String ACTIVE_CIRCLE_CLASS = "color-circle-active";

    private StackPane activeColorCircle;
    private ColorPicker threadColorPicker;

    private Image crossImage;
    private Color[][] stitchColors;
    private final Map<String, Image> tintCache = new HashMap<>();
    private Color currentThreadColor = Color.web("#D97757");

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

    private static final double DOT_RADIUS = 1.5;
    private static final double GRID_PADDING = 12.0;
    private static final Color GRID_DOT_COLOR = Color.web("#97958C", 0.6);

    private static final int GRID_MIN = 8;
    private static final int GRID_MAX = 96;
    private static final int GRID_STEP = 8;
    private int gridSize = 32;

    @FXML
    public void initialize() {
        simulateLoading();
        loadCrossImage();

        if (mainCanvasView != null && canvasContainer != null) {
            var squareSize = Bindings.min(
                    canvasContainer.widthProperty().subtract(40),
                    canvasContainer.heightProperty().subtract(40)
            );
            mainCanvasView.minWidthProperty().bind(squareSize);
            mainCanvasView.minHeightProperty().bind(squareSize);
            mainCanvasView.prefWidthProperty().bind(squareSize);
            mainCanvasView.prefHeightProperty().bind(squareSize);
            mainCanvasView.maxWidthProperty().bind(squareSize);
            mainCanvasView.maxHeightProperty().bind(squareSize);
        }

        if (gridCanvas != null && mainCanvasView != null) {
            gridCanvas.widthProperty().bind(mainCanvasView.widthProperty());
            gridCanvas.heightProperty().bind(mainCanvasView.heightProperty());
            gridCanvas.widthProperty().addListener((obs, oldVal, newVal) -> drawGrid());
            gridCanvas.heightProperty().addListener((obs, oldVal, newVal) -> drawGrid());
            drawGrid();
        }

        setupStitchLayer();
        resetStitches();

        if (createGridBtn != null && projectNameField != null) {
            projectNameField.setTextFormatter(new TextFormatter<String>(change -> {
                String next = change.getControlNewText();
                if (next.length() > 15) {
                    return null;
                }
                return next.matches("[A-Za-z0-9_-]*") ? change : null;
            }));
            createGridBtn.disableProperty().bind(
                    Bindings.createBooleanBinding(
                            () -> projectNameField.getText() == null || projectNameField.getText().trim().isEmpty(),
                            projectNameField.textProperty()
                    )
            );
            projectNameField.textProperty().addListener((obs, oldVal, newVal) -> {
                projectNameField.getStyleClass().remove("input-error");
            });
            createGridBtn.setOnAction(e -> {
                String name = projectNameField.getText() == null ? "" : projectNameField.getText().trim();
                if (name.isEmpty()) {
                    if (!projectNameField.getStyleClass().contains("input-error")) {
                        projectNameField.getStyleClass().add("input-error");
                    }
                    projectNameField.requestFocus();
                    return;
                }
                if (createMenu != null) {
                    createMenu.setManaged(false);
                    createMenu.setVisible(false);
                }
            });
        }

        if (gridSizeLabel != null) {
            gridSizeLabel.setText(gridSize + " x " + gridSize);
        }

        if (gridMinusBtn != null) {
            gridMinusBtn.setOnMouseClicked(e -> updateGridSize(-GRID_STEP));
        }
        if (gridPlusBtn != null) {
            gridPlusBtn.setOnMouseClicked(e -> updateGridSize(GRID_STEP));
        }

        activeNavLabel = (Label) mainApplicationLayout.lookup(".active-nav");
        if (activeNavLabel != null)
            setupNavHover(activeNavLabel, true);
        setupNavHover(saveNavLabel, false);
        setupNavHover(openNavLabel, false);
        setupNavHover(infoNavLabel, false);

        setupSymmetryButtonAnimations(horizontalSymmetryBox);
        setupSymmetryButtonAnimations(verticalSymmetryBox);

        setupSymmetryButtonAnimations(gridMinusBtn);
        setupSymmetryButtonAnimations(gridPlusBtn);

        initThreadColorPicker();

        setupColorCircleAnimations(colorCircle1, Color.web("#D97757"));
        setupColorCircleAnimations(colorCircle2, Color.web("#F4AAA9"));
        setupColorCircleAnimations(colorCircle3, Color.TRANSPARENT);
        setupColorCircleAnimations(colorCircle4, Color.TRANSPARENT);
        setupColorCircleAnimations(colorCircle5, Color.TRANSPARENT);
        setupColorCircleAnimations(colorCircle6, Color.TRANSPARENT);
        setupColorCircleAnimations(colorCircle7, Color.TRANSPARENT);
        setupColorCircleAnimations(colorCircle8, Color.TRANSPARENT);

        if (colorCircle1 != null) {
            setActiveThreadCircle(colorCircle1);
        }
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

    private void setupSymmetryButtonAnimations(javafx.scene.Node node) {
        node.setOnMouseEntered(e -> animateBackground(node, IDLE_COLOR, HOVER_COLOR, true));
        node.setOnMouseExited(e -> animateBackground(node, HOVER_COLOR, IDLE_COLOR, false));
    }

    private void setupColorCircleAnimations(StackPane pane, Color baseColor) {
        if (pane == null) return;
        boolean isEmpty = baseColor.equals(Color.TRANSPARENT);
        setCircleState(pane, baseColor, isEmpty);

        pane.setOnMouseEntered(e -> animateCircleHover(pane, getCircleColor(pane), isCircleEmpty(pane), true));
        pane.setOnMouseExited(e -> animateCircleHover(pane, getCircleColor(pane), isCircleEmpty(pane), false));
        pane.setOnMouseClicked(e -> {
            setActiveThreadCircle(pane);
            openThreadColorPicker(pane);
        });
    }

    private void initThreadColorPicker() {
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

    private void openThreadColorPicker(StackPane pane) {
        if (threadColorPicker == null || pane == null) {
            return;
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

    private String toRgb(Color color) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
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
                }
            }
        };
        transition.play();
    }

    private void animateBackground(javafx.scene.Node node, Color from, Color to, boolean isHover) {
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

    private void drawGrid() {
        if (gridCanvas == null) {
            return;
        }
        double width = gridCanvas.getWidth();
        double height = gridCanvas.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        GraphicsContext gc = gridCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        gc.setFill(GRID_DOT_COLOR);

        double innerWidth = Math.max(0, width - (GRID_PADDING * 2));
        double innerHeight = Math.max(0, height - (GRID_PADDING * 2));
        if (innerWidth <= 0 || innerHeight <= 0) {
            return;
        }

        double cellSize = Math.min(innerWidth, innerHeight) / gridSize;
        for (int row = 0; row <= gridSize; row++) {
            double y = GRID_PADDING + (row * cellSize);
            for (int col = 0; col <= gridSize; col++) {
                double x = GRID_PADDING + (col * cellSize);
                gc.fillOval(x - DOT_RADIUS, y - DOT_RADIUS, DOT_RADIUS * 2, DOT_RADIUS * 2);
            }
        }
    }

    private void simulateLoading() {
        if (loadingBar != null) {
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(loadingBar.progressProperty(), 0.0)),
                new KeyFrame(Duration.seconds(5), new KeyValue(loadingBar.progressProperty(), 1.0))
            );
            timeline.play();
        }

        PauseTransition delay = new PauseTransition(Duration.seconds(0));
        delay.setOnFinished(event -> {
            mainApplicationLayout.getChildren().remove(loadingScreen);
        });
        delay.play();
    }

    private void updateGridSize(int delta) {
        int next = Math.max(GRID_MIN, Math.min(GRID_MAX, gridSize + delta));
        if (next == gridSize) {
            return;
        }
        gridSize = next;
        if (gridSizeLabel != null) {
            gridSizeLabel.setText(gridSize + " x " + gridSize);
        }
        resetStitches();
        drawGrid();
    }

    private void loadCrossImage() {
        var url = getClass().getResource("/images/cross_sample.png");
        if (url != null) {
            crossImage = new Image(url.toExternalForm());
            tintCache.clear();
        }
    }

    private void setupStitchLayer() {
        if (stitchCanvas == null || mainCanvasView == null) {
            return;
        }
        stitchCanvas.widthProperty().bind(mainCanvasView.widthProperty());
        stitchCanvas.heightProperty().bind(mainCanvasView.heightProperty());
        stitchCanvas.widthProperty().addListener((obs, oldVal, newVal) -> drawStitches());
        stitchCanvas.heightProperty().addListener((obs, oldVal, newVal) -> drawStitches());
        stitchCanvas.setOnMouseClicked(e -> handleStitchClick(e.getX(), e.getY()));
    }

    private void resetStitches() {
        stitchColors = new Color[gridSize][gridSize];
        drawStitches();
    }

    private void handleStitchClick(double x, double y) {
        if (createMenu != null && createMenu.isVisible()) {
            return;
        }
        if (stitchCanvas == null || stitchColors == null) {
            return;
        }
        if (currentThreadColor == null || currentThreadColor.equals(Color.TRANSPARENT)) {
            return;
        }

        double width = stitchCanvas.getWidth();
        double height = stitchCanvas.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        double innerWidth = Math.max(0, width - (GRID_PADDING * 2));
        double innerHeight = Math.max(0, height - (GRID_PADDING * 2));
        if (innerWidth <= 0 || innerHeight <= 0) {
            return;
        }

        double cellSize = Math.min(innerWidth, innerHeight) / gridSize;
        double gx = x - GRID_PADDING;
        double gy = y - GRID_PADDING;
        if (gx < 0 || gy < 0) {
            return;
        }

        int col = (int) Math.floor(gx / cellSize);
        int row = (int) Math.floor(gy / cellSize);
        if (col < 0 || col >= gridSize || row < 0 || row >= gridSize) {
            return;
        }

        stitchColors[row][col] = currentThreadColor;
        drawStitches();
    }

    private void setActiveThreadCircle(StackPane pane) {
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

    private Image getTintedImage(Color color) {
        if (crossImage == null || crossImage.isError() || color == null) {
            return crossImage;
        }
        String key = String.format("#%02X%02X%02X:%03d",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255),
                (int) Math.round(color.getOpacity() * 255));
        Image cached = tintCache.get(key);
        if (cached != null) {
            return cached;
        }

        int width = (int) crossImage.getWidth();
        int height = (int) crossImage.getHeight();
        if (width <= 0 || height <= 0) {
            return crossImage;
        }

        WritableImage tinted = new WritableImage(width, height);
        PixelReader reader = crossImage.getPixelReader();
        PixelWriter writer = tinted.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color src = reader.getColor(x, y);
                double alpha = src.getOpacity();
                if (alpha <= 0.0) {
                    writer.setColor(x, y, Color.TRANSPARENT);
                    continue;
                }
                writer.setColor(x, y, new Color(
                        color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        alpha
                ));
            }
        }

        tintCache.put(key, tinted);
        return tinted;
    }

    private void drawStitches() {
        if (stitchCanvas == null) {
            return;
        }
        double width = stitchCanvas.getWidth();
        double height = stitchCanvas.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        GraphicsContext gc = stitchCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        if (crossImage == null || crossImage.isError() || stitchColors == null) {
            return;
        }

        double innerWidth = Math.max(0, width - (GRID_PADDING * 2));
        double innerHeight = Math.max(0, height - (GRID_PADDING * 2));
        if (innerWidth <= 0 || innerHeight <= 0) {
            return;
        }

        double cellSize = Math.min(innerWidth, innerHeight) / gridSize;
        double imageSize = Math.max(1, cellSize * 0.8);

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                Color color = stitchColors[row][col];
                if (color == null) {
                    continue;
                }
                Image paintImage = getTintedImage(color);
                double x = GRID_PADDING + (col * cellSize) + (cellSize - imageSize) / 2.0;
                double y = GRID_PADDING + (row * cellSize) + (cellSize - imageSize) / 2.0;
                gc.drawImage(paintImage, x, y, imageSize, imageSize);
            }
        }
    }
}
