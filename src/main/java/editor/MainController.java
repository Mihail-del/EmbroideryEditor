package editor;

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
    private final NavigationManager navManager = new NavigationManager();
    private StitchInteractionHandler stitchHandler;

    private boolean isVerticalSymmetryActive = false;
    private boolean isHorizontalSymmetryActive = false;

    private Runnable pendingAction;

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



        navManager.setupButtonHoverAnimation(horizontalSymmetryBox);
        navManager.setupButtonHoverAnimation(verticalSymmetryBox);

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

        Label activeNav = (Label) mainApplicationLayout.lookup(".active-nav");
        navManager.setActiveNavLabel(activeNav);
        if (activeNav != null)
            navManager.setupNavHover(activeNav, true);
        if (createNavLabel != null) {
            navManager.setupNavHover(createNavLabel, false);
            createNavLabel.setOnMouseClicked(e -> runWithSaveCheck(this::showCreateMenu));
        }
        navManager.setupNavHover(saveNavLabel, false);
        if (saveNavLabel != null) {
            saveNavLabel.setOnMouseClicked(e -> menuManager.showSaveOptionsMenu());
        }
        navManager.setupNavHover(openNavLabel, false);
        if (openNavLabel != null) {
            openNavLabel.setOnMouseClicked(e -> runWithSaveCheck(menuManager::showOpenMenu));
        }
        navManager.setupNavHover(infoNavLabel, false);
        if (infoNavLabel != null) {
            infoNavLabel.setOnMouseClicked(e -> menuManager.showInfoMenu());
        }

        navManager.setupButtonHoverAnimation(horizontalSymmetryBox);
        navManager.setupButtonHoverAnimation(verticalSymmetryBox);

        navManager.setupButtonHoverAnimation(gridMinusBtn);
        navManager.setupButtonHoverAnimation(gridPlusBtn);

        if (clearCanvasBtn != null) {
            clearCanvasBtn.setOnMouseClicked(e -> {
                if (stitchHandler != null) stitchHandler.clearCanvas();
            });
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

        stitchHandler = new StitchInteractionHandler(
                gridManager,
                () -> paletteManager != null ? paletteManager.getCurrentThreadColor() : null,
                () -> isVerticalSymmetryActive,
                () -> isHorizontalSymmetryActive,
                () -> menuManager != null && menuManager.isCreateMenuVisible(),
                this::drawStitches
        );
        stitchHandler.setupStitchLayer(stitchCanvas, mainCanvasView);
        stitchHandler.clearCanvas();

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
        if (gridManager.updateGridSize(delta)) {
            if (gridSizeLabel != null) {
                gridSizeLabel.setText(gridManager.getGridSize() + " x " + gridManager.getGridSize());
            }
            drawGrid();
            drawStitches();
        }
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

        if (stitchHandler != null) {
            stitchHandler.clearCanvas();
        }

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
        projectService.exportImage(snapshot, format, file);
    }

    private void showCreateMenu() {
        menuManager.hideAllMenus();
        if (stitchHandler != null) {
            stitchHandler.clearCanvas();
        }
        menuManager.showCreateMenu();
    }

    private void toggleEraser() {
        if (stitchHandler != null) {
            stitchHandler.toggleEraser(eraserBtn);
        }
    }


}
