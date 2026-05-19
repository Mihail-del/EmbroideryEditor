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
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.stage.FileChooser;

public class MainController {

    @FXML
    private StackPane mainApplicationLayout;

    @FXML
    private VBox loadingScreen;

    @FXML
    private ProgressBar loadingBar;

    private Label activeNavLabel;

    @FXML
    private Label createNavLabel;

    @FXML
    private Label saveNavLabel;

    @FXML
    private Label openNavLabel;

    @FXML
    private Label infoNavLabel;

    @FXML
    private Label clearCanvasBtn;

    @FXML
    private Label eraserBtn;

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

    private boolean isVerticalSymmetryActive = false;
    private boolean isHorizontalSymmetryActive = false;

    private boolean isEraserActive = false;
    private VBox saveWarningMenu;
    private VBox saveOptionsMenu;
    private Runnable pendingAction;

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

        setupSymmetryButtonAnimations(horizontalSymmetryBox);
        setupSymmetryButtonAnimations(verticalSymmetryBox);

        if (verticalSymmetryBox != null) {
            verticalSymmetryBox.setOnMouseClicked(e -> {
                isVerticalSymmetryActive = !isVerticalSymmetryActive;
                drawGrid();
            });
        }

        if (horizontalSymmetryBox != null) {
            horizontalSymmetryBox.setOnMouseClicked(e -> {
                isHorizontalSymmetryActive = !isHorizontalSymmetryActive;
                drawGrid();
            });
        }

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
        if (createNavLabel != null) {
            setupNavHover(createNavLabel, false);
            createNavLabel.setOnMouseClicked(e -> runWithSaveCheck(this::showCreateMenu));
        }
        setupNavHover(saveNavLabel, false);
        if (saveNavLabel != null) {
            saveNavLabel.setOnMouseClicked(e -> showSaveOptionsMenu());
        }
        setupNavHover(openNavLabel, false);
        if (openNavLabel != null) {
            openNavLabel.setOnMouseClicked(e -> runWithSaveCheck(this::openProject));
        }
        setupNavHover(infoNavLabel, false);

        setupSymmetryButtonAnimations(horizontalSymmetryBox);
        setupSymmetryButtonAnimations(verticalSymmetryBox);

        setupSymmetryButtonAnimations(gridMinusBtn);
        setupSymmetryButtonAnimations(gridPlusBtn);

        if (clearCanvasBtn != null) {
            clearCanvasBtn.setOnMouseClicked(e -> clearCanvas());
        }

        if (eraserBtn != null) {
            eraserBtn.setOnMouseClicked(e -> toggleEraser());
        }

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

        initSaveWarningMenu();
        initSaveOptionsMenu();

        Timeline autoSaveTimeline = new Timeline(
            new KeyFrame(Duration.seconds(10), e -> {
                if (!isCanvasClear()) {
                    saveProject();
                }
            })
        );
        autoSaveTimeline.setCycleCount(Timeline.INDEFINITE);
        autoSaveTimeline.play();
    }

    private void initSaveWarningMenu() {
        if (mainCanvasView == null) return;
        saveWarningMenu = new VBox(20);
        saveWarningMenu.getStyleClass().add("warning-menu");
        saveWarningMenu.setAlignment(javafx.geometry.Pos.CENTER);

        Label warningLabel = new Label("You have unsaved changes.\nDo you want to save before proceeding?");
        warningLabel.setWrapText(true);
        warningLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        warningLabel.getStyleClass().add("warning-label");

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().addAll("create-grid-btn", "warning-save-btn");
        saveBtn.setOnAction(e -> {
            saveProject();
            hideWarningMenu();
            if (pendingAction != null) pendingAction.run();
        });

        Button dontSaveBtn = new Button("Don't Save");
        dontSaveBtn.getStyleClass().addAll("create-grid-btn", "warning-dont-save-btn");
        dontSaveBtn.setOnAction(e -> {
            hideWarningMenu();
            if (pendingAction != null) pendingAction.run();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("create-grid-btn", "warning-cancel-btn");
        cancelBtn.setOnAction(e -> {
            hideWarningMenu();
            pendingAction = null;
        });

        HBox btns = new HBox(15, saveBtn, dontSaveBtn, cancelBtn);
        btns.setAlignment(javafx.geometry.Pos.CENTER);

        saveWarningMenu.getChildren().addAll(warningLabel, btns);
        saveWarningMenu.setVisible(false);
        saveWarningMenu.setManaged(false);

        mainCanvasView.getChildren().add(saveWarningMenu);
    }

    private void initSaveOptionsMenu() {
        if (mainCanvasView == null) return;
        saveOptionsMenu = new VBox(20);
        saveOptionsMenu.getStyleClass().add("save-options-menu");
        saveOptionsMenu.setAlignment(javafx.geometry.Pos.CENTER);

        Label titleLabel = new Label("Save Options");
        titleLabel.getStyleClass().add("warning-label");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");

        // Template Block
        VBox templateBlock = new VBox(10);
        templateBlock.setAlignment(javafx.geometry.Pos.CENTER);
        templateBlock.getStyleClass().add("save-block-frame");
        Label templateLabel = new Label("Save as Template");
        templateLabel.getStyleClass().add("warning-label");

        Button jsonBtn = new Button(".JSON");
        jsonBtn.getStyleClass().addAll("create-grid-btn");
        jsonBtn.setPrefWidth(240);
        jsonBtn.setOnAction(e -> {
            hideSaveOptionsMenu();
            saveProjectAsJson();
        });
        templateBlock.getChildren().addAll(templateLabel, jsonBtn);

        // Image Block
        VBox imageBlock = new VBox(10);
        imageBlock.setAlignment(javafx.geometry.Pos.CENTER);
        imageBlock.getStyleClass().add("save-block-frame");
        Label imageLabel = new Label("Save as Image");
        imageLabel.getStyleClass().add("warning-label");

        HBox imageBtns = new HBox(15);
        imageBtns.setAlignment(javafx.geometry.Pos.CENTER);

        Button pngBtn = new Button(".PNG");
        pngBtn.getStyleClass().addAll("create-grid-btn");
        pngBtn.setStyle("-fx-padding: 10px 15px; -fx-max-width: 80px; -fx-font-size: 14px;");
        pngBtn.setOnAction(e -> {
            hideSaveOptionsMenu();
            saveProjectAsImage("png");
        });

        Button jpgBtn = new Button(".JPG");
        jpgBtn.getStyleClass().addAll("create-grid-btn");
        jpgBtn.setStyle("-fx-padding: 10px 15px; -fx-max-width: 80px; -fx-font-size: 14px;");
        jpgBtn.setOnAction(e -> {
            hideSaveOptionsMenu();
            saveProjectAsImage("jpg");
        });

        Button gifBtn = new Button(".GIF");
        gifBtn.getStyleClass().addAll("create-grid-btn");
        gifBtn.setStyle("-fx-padding: 10px 15px; -fx-max-width: 80px; -fx-font-size: 14px;");
        gifBtn.setOnAction(e -> {
            hideSaveOptionsMenu();
            saveProjectAsImage("gif");
        });

        imageBtns.getChildren().addAll(pngBtn, jpgBtn, gifBtn);
        imageBlock.getChildren().addAll(imageLabel, imageBtns);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("create-grid-btn", "warning-cancel-btn");
        cancelBtn.setPrefWidth(120);
        cancelBtn.setOnAction(e -> hideSaveOptionsMenu());

        saveOptionsMenu.getChildren().addAll(titleLabel, templateBlock, imageBlock, cancelBtn);
        saveOptionsMenu.setVisible(false);
        saveOptionsMenu.setManaged(false);

        mainCanvasView.getChildren().add(saveOptionsMenu);
    }

    private void showSaveOptionsMenu() {
        if (saveOptionsMenu != null) {
            saveOptionsMenu.setVisible(true);
            saveOptionsMenu.setManaged(true);
        }
    }

    private void hideSaveOptionsMenu() {
        if (saveOptionsMenu != null) {
            saveOptionsMenu.setVisible(false);
            saveOptionsMenu.setManaged(false);
        }
    }

    private void hideWarningMenu() {
        if (saveWarningMenu != null) {
            saveWarningMenu.setVisible(false);
            saveWarningMenu.setManaged(false);
        }
    }

    private boolean isCanvasClear() {
        if (stitchColors == null) return true;
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                if (stitchColors[r][c] != null) return false;
            }
        }
        return true;
    }

    private void runWithSaveCheck(Runnable action) {
        if (createMenu != null && createMenu.isVisible()) {
            action.run();
            return;
        }
        if (isCanvasClear()) {
            action.run();
        } else {
            pendingAction = action;
            if (saveWarningMenu != null) {
                saveWarningMenu.setVisible(true);
                saveWarningMenu.setManaged(true);
            }
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
        return String.format(Locale.US, "rgba(%d, %d, %d, %.3f)",
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
            if (isCircleEmpty(pane)) {
                // If it's an empty slot (+), any mouse click makes it active and shows the palette
                setActiveThreadCircle(pane);
                openThreadColorPicker(pane);
            } else {
                // If it already has a color
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    // Left click: Only make it active
                    setActiveThreadCircle(pane);
                } else if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                    // Right click: Make it active AND open palette to replace the color
                    setActiveThreadCircle(pane);
                    openThreadColorPicker(pane);
                }
            }
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

        double gridTotalWidth = gridSize * cellSize;
        double gridTotalHeight = gridSize * cellSize;

        if (isVerticalSymmetryActive || isHorizontalSymmetryActive) {
            gc.setStroke(Color.web("#D97757", 0.8)); // Accent color
            gc.setLineWidth(2.0);
            gc.setLineDashes(8, 8); // Dashed effect

            if (isVerticalSymmetryActive) {
                double exactMiddleX = GRID_PADDING + (gridTotalWidth / 2.0);
                gc.strokeLine(exactMiddleX, GRID_PADDING, exactMiddleX, GRID_PADDING + gridTotalHeight);
            }

            if (isHorizontalSymmetryActive) {
                double exactMiddleY = GRID_PADDING + (gridTotalHeight / 2.0);
                gc.strokeLine(GRID_PADDING, exactMiddleY, GRID_PADDING + gridTotalWidth, exactMiddleY);
            }

            gc.setLineDashes((double[]) null);
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

    private void clearCanvas() {
        resetStitches();
    }

    private void saveProject() {
        String projectName = (projectNameField != null && projectNameField.getText() != null && !projectNameField.getText().trim().isEmpty())
                             ? projectNameField.getText().trim()
                             : "project";

        Map<String, Object> projectData = new HashMap<>();
        projectData.put("gridSize", gridSize);

        List<Map<String, Object>> stitches = new ArrayList<>();
        if (stitchColors != null) {
            for (int r = 0; r < gridSize; r++) {
                for (int c = 0; c < gridSize; c++) {
                    if (stitchColors[r][c] != null) {
                        Map<String, Object> stitch = new HashMap<>();
                        stitch.put("row", r);
                        stitch.put("col", c);
                        stitch.put("color", toRgba(stitchColors[r][c]));
                        stitches.add(stitch);
                    }
                }
            }
        }
        projectData.put("stitches", stitches);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(projectData);

        try {
            File dir = new File("src/main/resources/templates");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, projectName + ".json");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json);
            }
            System.out.println("Auto-saved project to: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveProjectAsJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Project JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        String projectName = (projectNameField != null && projectNameField.getText() != null && !projectNameField.getText().trim().isEmpty())
                             ? projectNameField.getText().trim()
                             : "project";
        fileChooser.setInitialFileName(projectName + ".json");

        File file = fileChooser.showSaveDialog(mainApplicationLayout.getScene().getWindow());
        if (file != null) {
            Map<String, Object> projectData = new HashMap<>();
            projectData.put("gridSize", gridSize);

            List<Map<String, Object>> stitches = new ArrayList<>();
            if (stitchColors != null) {
                for (int r = 0; r < gridSize; r++) {
                    for (int c = 0; c < gridSize; c++) {
                        if (stitchColors[r][c] != null) {
                            Map<String, Object> stitch = new HashMap<>();
                            stitch.put("row", r);
                            stitch.put("col", c);
                            stitch.put("color", toRgba(stitchColors[r][c]));
                            stitches.add(stitch);
                        }
                    }
                }
            }
            projectData.put("stitches", stitches);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(gson.toJson(projectData));
                System.out.println("Manually saved project JSON to: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveProjectAsImage(String format) {
        if (mainCanvasView == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Image as " + format.toUpperCase());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(format.toUpperCase() + " Image", "*." + format));

        String projectName = (projectNameField != null && projectNameField.getText() != null && !projectNameField.getText().trim().isEmpty())
                             ? projectNameField.getText().trim()
                             : "project";
        fileChooser.setInitialFileName(projectName + "." + format);

        File file = fileChooser.showSaveDialog(mainApplicationLayout.getScene().getWindow());
        if (file != null) {
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            WritableImage snapshot = mainCanvasView.snapshot(params, null);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

            // For formats like JPG that don't support alpha, we might need a background.
            if (format.equalsIgnoreCase("jpg") || format.equalsIgnoreCase("jpeg")) {
                BufferedImage jpgImage = new BufferedImage(
                    bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g2d = jpgImage.createGraphics();
                g2d.setColor(java.awt.Color.WHITE);
                g2d.fillRect(0, 0, jpgImage.getWidth(), jpgImage.getHeight());
                g2d.drawImage(bufferedImage, 0, 0, null);
                g2d.dispose();
                bufferedImage = jpgImage;
            }

            try {
                ImageIO.write(bufferedImage, format, file);
                System.out.println("Exported Image to: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void openProject() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Open Project");
        File initialDir = new File("src/main/resources/templates");
        if (initialDir.exists()) {
            fileChooser.setInitialDirectory(initialDir);
        }
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File selectedFile = fileChooser.showOpenDialog(mainApplicationLayout.getScene().getWindow());
        if (selectedFile != null) {
            try {
                String content = new String(Files.readAllBytes(selectedFile.toPath()));
                Gson gson = new Gson();
                Map<String, Object> projectData = gson.fromJson(content, Map.class);

                if (projectData.containsKey("gridSize")) {
                    gridSize = Math.max(GRID_MIN, Math.min(GRID_MAX, ((Double) projectData.get("gridSize")).intValue()));
                    if (gridSizeLabel != null) {
                        gridSizeLabel.setText(gridSize + " x " + gridSize);
                    }
                }

                resetStitches();

                if (projectData.containsKey("stitches")) {
                    List<Map<String, Object>> stitches = (List<Map<String, Object>>) projectData.get("stitches");
                    for (Map<String, Object> stitch : stitches) {
                        int r = ((Double) stitch.get("row")).intValue();
                        int c = ((Double) stitch.get("col")).intValue();
                        String colorStr = (String) stitch.get("color");
                        if (r >= 0 && r < gridSize && c >= 0 && c < gridSize && colorStr != null) {
                            stitchColors[r][c] = parseRgba(colorStr);
                        }
                    }
                }

                String name = selectedFile.getName();
                if (name.endsWith(".json")) {
                    name = name.substring(0, name.length() - 5);
                }
                if (projectNameField != null) {
                    projectNameField.setText(name);
                }

                if (createMenu != null) {
                    createMenu.setManaged(false);
                    createMenu.setVisible(false);
                }

                drawGrid();
                drawStitches();
                System.out.println("Opened project from: " + selectedFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Color parseRgba(String rgba) {
        if (rgba == null || rgba.isEmpty()) return Color.TRANSPARENT;
        if (rgba.startsWith("rgba(")) {
            String inner = rgba.substring(5, rgba.length() - 1);
            String[] parts = inner.split(",");
            if (parts.length >= 4) {
                try {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    double a = Double.parseDouble(parts[3].trim());
                    return Color.rgb(r, g, b, a);
                } catch (NumberFormatException e) {
                    // Fallback to web
                }
            }
        }
        try {
            return Color.web(rgba);
        } catch (Exception e) {
            return Color.TRANSPARENT;
        }
    }

    private void showCreateMenu() {
        clearCanvas();
        if (createMenu != null) {
            createMenu.setManaged(true);
            createMenu.setVisible(true);
        }
    }

    private void toggleEraser() {
        isEraserActive = !isEraserActive;
        if (eraserBtn != null) {
            if (isEraserActive) {
                eraserBtn.getStyleClass().add("eraser-active");
            } else {
                eraserBtn.getStyleClass().remove("eraser-active");
            }
        }
    }

    private void handleStitchClick(double x, double y) {
        if (createMenu != null && createMenu.isVisible()) {
            return;
        }
        if (stitchCanvas == null || stitchColors == null) {
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

        if (isEraserActive) {
            // Eraser mode: remove stitches
            stitchColors[row][col] = null;
            if (isVerticalSymmetryActive) {
                stitchColors[row][(gridSize - 1) - col] = null;
            }
            if (isHorizontalSymmetryActive) {
                stitchColors[(gridSize - 1) - row][col] = null;
            }
            if (isVerticalSymmetryActive && isHorizontalSymmetryActive) {
                stitchColors[(gridSize - 1) - row][(gridSize - 1) - col] = null;
            }
        } else {
            // Normal mode: add stitches
            if (currentThreadColor == null || currentThreadColor.equals(Color.TRANSPARENT)) {
                return;
            }
            stitchColors[row][col] = currentThreadColor;
            if (isVerticalSymmetryActive) {
                stitchColors[row][(gridSize - 1) - col] = currentThreadColor;
            }
            if (isHorizontalSymmetryActive) {
                stitchColors[(gridSize - 1) - row][col] = currentThreadColor;
            }
            if (isVerticalSymmetryActive && isHorizontalSymmetryActive) {
                stitchColors[(gridSize - 1) - row][(gridSize - 1) - col] = currentThreadColor;
            }
        }

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
