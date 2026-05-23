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
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.beans.binding.Bindings;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.WritableImage;
import javafx.scene.SnapshotParameters;
import java.io.File;

import static editor.GridManager.*;

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
    private Label closeCreateMenuBtn;

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

    private final GridManager gridManager = new GridManager();
    private final ProjectService projectService = new ProjectService();
    private RenderEngine renderEngine;
    private MenuManager menuManager;
    private ThreadPaletteManager paletteManager;

    private static final Color NAV_IDLE_TEXT = Color.web("#F9F9F7");
    private static final Color NAV_ACTIVE_TEXT = Color.web("#D97757");
    private static final Color NAV_BORDER_ACTIVE = Color.web("#D97757");
    private static final Color NAV_BORDER_TRANSPARENT = Color.web("#D97757", 0.0);

    private static final String NAV_STATE_KEY = "navState";

    private boolean isVerticalSymmetryActive = false;
    private boolean isHorizontalSymmetryActive = false;

    private boolean isEraserActive = false;
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

    @FXML
    public void initialize() {
        simulateLoading();

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

        if (closeCreateMenuBtn != null) {
            closeCreateMenuBtn.setOnMouseClicked(e -> menuManager.hideCreateMenu());
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
                menuManager.hideCreateMenu();
            });
        }

        if (gridSizeLabel != null) {
            gridSizeLabel.setText(gridManager.getGridSize() + " x " + gridManager.getGridSize());
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
            saveNavLabel.setOnMouseClicked(e -> menuManager.showSaveOptionsMenu());
        }
        setupNavHover(openNavLabel, false);
        if (openNavLabel != null) {
            openNavLabel.setOnMouseClicked(e -> runWithSaveCheck(menuManager::showOpenMenu));
        }
        setupNavHover(infoNavLabel, false);
        if (infoNavLabel != null) {
            infoNavLabel.setOnMouseClicked(e -> menuManager.showInfoMenu());
        }

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

        paletteManager = new ThreadPaletteManager(mainApplicationLayout);
        paletteManager.initColorPicker();

        menuManager = new MenuManager(mainCanvasView, createMenu, paletteManager.getColorPicker(), new MenuManager.MenuCallbacks() {
            @Override
            public void onSaveAsJson() {
                projectService.saveProjectAsJson(getProjectName(), gridManager,
                        mainApplicationLayout.getScene().getWindow());
            }

            @Override
            public void onExportImage(String format, boolean transparentBg) {
                handleImageExport(format, transparentBg);
            }

            @Override
            public void onBrowseOpenFile() {
                openProjectFromFileChooser();
            }

            @Override
            public void onLoadRecentProject(File file) {
                loadProjectFromFile(file);
            }

            @Override
            public void onHideAllMenus() {
                menuManager.hideAllMenus();
            }
        });

        paletteManager.setHideAllMenusCallback(() -> menuManager.hideAllMenus());

        paletteManager.setupColorCircle(colorCircle1, Color.web("#D97757"));
        paletteManager.setupColorCircle(colorCircle2, Color.web("#F4AAA9"));
        paletteManager.setupColorCircle(colorCircle3, Color.TRANSPARENT);
        paletteManager.setupColorCircle(colorCircle4, Color.TRANSPARENT);
        paletteManager.setupColorCircle(colorCircle5, Color.TRANSPARENT);
        paletteManager.setupColorCircle(colorCircle6, Color.TRANSPARENT);
        paletteManager.setupColorCircle(colorCircle7, Color.TRANSPARENT);
        paletteManager.setupColorCircle(colorCircle8, Color.TRANSPARENT);

        if (colorCircle1 != null) {
            paletteManager.setActiveCircle(colorCircle1);
        }

        menuManager.initAllMenus();

        Timeline autoSaveTimeline = new Timeline(
            new KeyFrame(Duration.seconds(10), e -> {
                if (!isCanvasClear()) {
                    String projectName = getProjectName();
                    projectService.saveProject(projectName, gridManager);
                }
            })
        );
        autoSaveTimeline.setCycleCount(Timeline.INDEFINITE);
        autoSaveTimeline.play();

        this.renderEngine = new RenderEngine(gridCanvas, stitchCanvas, gridManager);

        if (renderEngine != null) {
            renderEngine.drawGrid(isVerticalSymmetryActive, isHorizontalSymmetryActive);
            renderEngine.drawStitches();
        }
    }

    private void drawGrid() {
        if (renderEngine != null) {
            renderEngine.drawGrid(isVerticalSymmetryActive, isHorizontalSymmetryActive);
        }
    }

    private void drawStitches() {
        if (renderEngine != null) {
            renderEngine.drawStitches();
        }
    }



    private boolean isCanvasClear() {
        return gridManager.isCanvasClear();
    }

    private void runWithSaveCheck(Runnable action) {
        if (menuManager.isCreateMenuVisible()) {
            action.run();
            return;
        }
        if (!isCanvasClear()) {
            projectService.saveProject(getProjectName(), gridManager);
        }
        action.run();
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
        return ProjectService.toRgba(color);
    }

    private void setupSymmetryButtonAnimations(javafx.scene.Node node) {
        node.setOnMouseEntered(e -> animateBackground(node, IDLE_COLOR, HOVER_COLOR, true));
        node.setOnMouseExited(e -> animateBackground(node, HOVER_COLOR, IDLE_COLOR, false));
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

    private void growGrid() {
        if (gridManager.incrementGridSize()) {
            onGridSizeChanged();
        }
    }

    private void shrinkGrid() {
        if (gridManager.decrementGridSize()) {
            onGridSizeChanged();
        }
    }

    private void onGridSizeChanged() {
        if (gridSizeLabel != null) {
            gridSizeLabel.setText(gridManager.getGridSize() + " x " + gridManager.getGridSize());
        }
        drawGrid();
        drawStitches();
    }

    /**
     * Increases the grid size by the specified step (GRID_STEP)
     */
    public void incrementGridSize() {
        updateGridSize(GRID_STEP);
    }

    /**
     * Reduces the grid size by the specified step (GRID_STEP)
     */
    public void decrementGridSize() {
        updateGridSize(-GRID_STEP);
    }

    private void updateGridSize(int delta) {
        if (gridManager.updateGridSize(delta)) {
            if (gridSizeLabel != null) {
                gridSizeLabel.setText(gridManager.getGridSize() + " x " + gridManager.getGridSize());
            }
            drawGrid();
            drawStitches();
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
        gridManager.resetStitches();
        drawStitches();
    }

    private void clearCanvas() {
        resetStitches();
    }

    private String getProjectName() {
        return (projectNameField != null && projectNameField.getText() != null && !projectNameField.getText().trim().isEmpty())
                ? projectNameField.getText().trim()
                : "project";
    }

    private void openProjectFromFileChooser() {
        File selectedFile = projectService.chooseProjectFile(mainApplicationLayout.getScene().getWindow());
        if (selectedFile != null) {
            loadProjectFromFile(selectedFile);
        }
    }

    private void loadProjectFromFile(File selectedFile) {
        ProjectData data = projectService.loadProjectFromFile(selectedFile);
        if (data == null) return;

        gridManager.setGridSize(data.getGridSize());
        if (gridSizeLabel != null) {
            gridSizeLabel.setText(gridManager.getGridSize() + " x " + gridManager.getGridSize());
        }

        resetStitches();

        for (ProjectData.StitchData stitch : data.getStitches()) {
            if (gridManager.isValidCoordinate(stitch.row(), stitch.col()) && stitch.color() != null) {
                gridManager.setStitchColor(stitch.row(), stitch.col(), stitch.color());
            }
        }

        if (projectNameField != null) {
            projectNameField.setText(data.getProjectName());
        }

        menuManager.hideCreateMenu();

        drawGrid();
        drawStitches();
    }

    private void handleImageExport(String format, boolean isTransparentBg) {
        if (mainCanvasView == null) return;

        File file = projectService.chooseImageExportFile(format, getProjectName(),
                mainApplicationLayout.getScene().getWindow());
        if (file == null) return;

        // Temporarily disable symmetry lines for clean export
        boolean prevVertical = isVerticalSymmetryActive;
        boolean prevHorizontal = isHorizontalSymmetryActive;
        isVerticalSymmetryActive = false;
        isHorizontalSymmetryActive = false;
        drawGrid();

        String originalStyle = mainCanvasView.getStyle();
        boolean originalGridVisible = gridCanvas.isVisible();

        if (isTransparentBg) {
            mainCanvasView.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-effect: none;");
            gridCanvas.setVisible(false);
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(javafx.scene.paint.Color.TRANSPARENT);
        WritableImage snapshot = mainCanvasView.snapshot(params, null);

        if (isTransparentBg) {
            mainCanvasView.setStyle(originalStyle != null ? originalStyle : "");
            gridCanvas.setVisible(originalGridVisible);
        }

        isVerticalSymmetryActive = prevVertical;
        isHorizontalSymmetryActive = prevHorizontal;
        drawGrid();

        // Delegate image writing to ProjectService
        projectService.exportImage(snapshot, format, file);
    }

    private void showCreateMenu() {
        menuManager.hideAllMenus();
        clearCanvas();
        menuManager.showCreateMenu();
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
        if (menuManager.isCreateMenuVisible()) {
            return;
        }
        if (stitchCanvas == null) {
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

        double cellSize = Math.min(innerWidth, innerHeight) / gridManager.getGridSize();
        double gx = x - GRID_PADDING;
        double gy = y - GRID_PADDING;
        if (gx < 0 || gy < 0) {
            return;
        }

        int col = (int) Math.floor(gx / cellSize);
        int row = (int) Math.floor(gy / cellSize);
        if (col < 0 || col >= gridManager.getGridSize() || row < 0 || row >= gridManager.getGridSize()) {
            return;
        }

        if (isEraserActive) {
            gridManager.applyStitchWithSymmetry(row, col, null, isVerticalSymmetryActive, isHorizontalSymmetryActive);
        } else {
            Color threadColor = paletteManager.getCurrentThreadColor();
            if (threadColor == null || threadColor.equals(Color.TRANSPARENT)) {
                return;
            }
            gridManager.applyStitchWithSymmetry(row, col, threadColor, isVerticalSymmetryActive, isHorizontalSymmetryActive);
        }
        drawStitches();
    }


}
