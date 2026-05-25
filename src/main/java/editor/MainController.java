package editor;

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
    private Label autoSaveLabel;

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
    private Label animateBtn;

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
    private HBox duplicateLeftBtn;

    @FXML
    private HBox duplicateRightBtn;

    @FXML
    private HBox duplicateUpBtn;

    @FXML
    private HBox duplicateDownBtn;

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
    private NotificationManager notificationManager;
    private StitchAnimator stitchAnimator;

    private boolean isVerticalSymmetryActive = false;
    private boolean isHorizontalSymmetryActive = false;

    private Runnable pendingAction;

    @FXML
    public void initialize() {
        simulateLoading();

        if (mainCanvasView != null && canvasContainer != null) {
            var squareSize = Bindings.min(
                    canvasContainer.widthProperty().subtract(40),
                    canvasContainer.heightProperty().subtract(40));
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
                if (isVerticalSymmetryActive) {
                    if (!verticalSymmetryBox.getStyleClass().contains("symmetry-active")) {
                        verticalSymmetryBox.getStyleClass().add("symmetry-active");
                    }
                } else {
                    verticalSymmetryBox.getStyleClass().remove("symmetry-active");
                }
                drawGrid();
            });
        }

        if (horizontalSymmetryBox != null) {
            horizontalSymmetryBox.setOnMouseClicked(e -> {
                isHorizontalSymmetryActive = !isHorizontalSymmetryActive;
                if (isHorizontalSymmetryActive) {
                    if (!horizontalSymmetryBox.getStyleClass().contains("symmetry-active")) {
                        horizontalSymmetryBox.getStyleClass().add("symmetry-active");
                    }
                } else {
                    horizontalSymmetryBox.getStyleClass().remove("symmetry-active");
                }
                drawGrid();
            });
        }

        setupDuplicateButton(duplicateLeftBtn, "left");
        setupDuplicateButton(duplicateRightBtn, "right");
        setupDuplicateButton(duplicateUpBtn, "up");
        setupDuplicateButton(duplicateDownBtn, "down");

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
                            projectNameField.textProperty()));
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
                if (stitchHandler != null)
                    stitchHandler.clearCanvas();
            });
        }

        if (eraserBtn != null) {
            eraserBtn.setOnMouseClicked(e -> toggleEraser());
        }

        paletteManager = new ThreadPaletteManager(mainApplicationLayout);
        paletteManager.initColorPicker();

        menuManager = new MenuManager(mainCanvasView, createMenu, paletteManager.getColorPicker(),
                new MenuManager.MenuCallbacks() {
                    @Override
                    public void onSaveAsJson() {
                        projectService.saveProjectAsJson(getProjectName(), gridManager,
                                mainApplicationLayout.getScene().getWindow());
                    }

                    @Override
                    public void onExportImage(String format, boolean transparentBg, boolean animated) {
                        handleImageExport(format, transparentBg, animated);
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
                this::drawStitches);
        stitchHandler.setupStitchLayer(stitchCanvas, mainCanvasView);
        stitchHandler.clearCanvas();

        Timeline autoSaveTimeline = new Timeline(
                new KeyFrame(Duration.seconds(10), e -> {
                    if (!isCanvasClear()) {
                        autoSaveProject();
                    }
                }));
        autoSaveTimeline.setCycleCount(Timeline.INDEFINITE);
        autoSaveTimeline.play();

        this.notificationManager = new NotificationManager(autoSaveLabel);

        this.renderEngine = new RenderEngine(gridCanvas, stitchCanvas, gridManager);

        this.stitchAnimator = new StitchAnimator();
        renderEngine.setAnimator(stitchAnimator);

        if (renderEngine != null) {
            renderEngine.drawGrid(isVerticalSymmetryActive, isHorizontalSymmetryActive);
            renderEngine.drawStitches();
        }

        if (animateBtn != null) {
            animateBtn.setOnMouseClicked(e -> toggleAnimation());
        }

        // Load the start template
        loadStartTemplate();
    }

    private void loadStartTemplate() {
        File startTemplate = projectService.prepareStartTemplate();
        if (startTemplate != null) {
            loadProjectFromFile(startTemplate);
        }
        menuManager.hideCreateMenu();
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
            autoSaveProject();
        }
        action.run();
    }

    private void autoSaveProject() {
        projectService.saveProject(getProjectName(), gridManager);
        if (notificationManager != null) {
            notificationManager.showAutoSaveNotification();
        }
    }

    private void simulateLoading() {
        if (loadingBar != null) {
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(loadingBar.progressProperty(), 0.0)),
                new KeyFrame(Duration.seconds(2), new KeyValue(loadingBar.progressProperty(), 1.0))
            );
            timeline.setDelay(Duration.seconds(1));
            timeline.play();
        }

        if (loadingScreen == null || mainApplicationLayout == null) return;

        final boolean[] dismissed = {false};
        Runnable hideLoading = () -> {
            if (dismissed[0]) return;
            dismissed[0] = true;
            mainApplicationLayout.getChildren().remove(loadingScreen);
            loadingScreen.setOnMouseClicked(null);
            loadingScreen.setOnKeyPressed(null);
        };

        loadingScreen.setOnMouseClicked(e -> hideLoading.run());
        loadingScreen.setOnKeyPressed(e -> hideLoading.run());
        loadingScreen.setFocusTraversable(true);
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
        return (projectNameField != null && projectNameField.getText() != null
                && !projectNameField.getText().trim().isEmpty())
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
        if (data == null)
            return;

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

    private void handleImageExport(String format, boolean isTransparentBg, boolean animated) {
        if (mainCanvasView == null)
            return;

        File file = projectService.chooseImageExportFile(format, getProjectName(),
                mainApplicationLayout.getScene().getWindow());
        if (file == null)
            return;

        if (animated && format.equalsIgnoreCase("gif")) {
            exportAnimatedGif(file, isTransparentBg);
            return;
        }

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

    private void exportAnimatedGif(File file, boolean isTransparentBg) {
        if (mainCanvasView == null) return;
        
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

        StitchAnimator exporterAnimator = new StitchAnimator();
        exporterAnimator.setup(gridManager);
        
        StitchAnimator originalAnimator = this.stitchAnimator;
        renderEngine.setAnimator(exporterAnimator);

        try {
            javax.imageio.stream.ImageOutputStream output = new javax.imageio.stream.FileImageOutputStream(file);
            int delayMs = 50; // 20 fps
            editor.util.GifSequenceWriter writer = new editor.util.GifSequenceWriter(output, java.awt.image.BufferedImage.TYPE_INT_ARGB, delayMs, true);

            double totalNs = exporterAnimator.getTotalDurationNs();
            double stepNs = delayMs * 1_000_000.0;

            for (double t = 0; t <= totalNs + stepNs; t += stepNs) {
                exporterAnimator.setElapsed(t);
                renderEngine.drawStitches();
                
                WritableImage fxImage = mainCanvasView.snapshot(params, null);
                java.awt.image.BufferedImage awtImage = javafx.embed.swing.SwingFXUtils.fromFXImage(fxImage, null);
                
                if (!isTransparentBg) {
                    java.awt.image.BufferedImage rgbImage = new java.awt.image.BufferedImage(
                            awtImage.getWidth(), awtImage.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D g2d = rgbImage.createGraphics();
                    g2d.setColor(java.awt.Color.WHITE);
                    g2d.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
                    g2d.drawImage(awtImage, 0, 0, null);
                    g2d.dispose();
                    awtImage = rgbImage;
                }

                writer.writeToSequence(awtImage);
            }
            writer.close();
            output.close();
            System.out.println("Exported Animated GIF to: " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (isTransparentBg) {
                mainCanvasView.setStyle(originalStyle != null ? originalStyle : "");
                gridCanvas.setVisible(originalGridVisible);
            }
            isVerticalSymmetryActive = prevVertical;
            isHorizontalSymmetryActive = prevHorizontal;
            renderEngine.setAnimator(originalAnimator);
            drawGrid();
            drawStitches();
        }
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

    private void toggleAnimation() {
        if (stitchAnimator == null || renderEngine == null) return;

        if (stitchAnimator.isAnimating()) {
            stitchAnimator.stop();
            drawStitches();
            setAnimateButtonActive(false);
        } else {
            if (isCanvasClear()) return;
            setAnimateButtonActive(true);
            stitchAnimator.play(gridManager, this::drawStitches, () -> {
                setAnimateButtonActive(false);
                drawStitches();
            });
        }
    }

    private void setAnimateButtonActive(boolean active) {
        if (animateBtn == null) return;
        if (active) {
            animateBtn.setText("Stop");
            if (!animateBtn.getStyleClass().contains("animate-btn-active")) {
                animateBtn.getStyleClass().add("animate-btn-active");
            }
        } else {
            animateBtn.setText("Animate");
            animateBtn.getStyleClass().remove("animate-btn-active");
        }
    }

    /**
     * Sets up a duplicate direction button with:
     * - Hover background animation (matching symmetry button style)
     * - Live semi-transparent preview of the duplication result
     * - Click to permanently apply the duplication
     */
    private void setupDuplicateButton(HBox btn, String direction) {
        if (btn == null) return;

        final String HOVER_STYLE = "-fx-background-color: #1f1f1e; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 10, 0, 0, 2);";
        final String IDLE_STYLE  = "-fx-background-color: #252524;";

        btn.setOnMouseEntered(e -> {
            btn.setStyle(HOVER_STYLE);
            if (renderEngine != null) {
                renderEngine.setPreviewDirection(direction);
                drawStitches();
            }
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(IDLE_STYLE);
            if (renderEngine != null) {
                renderEngine.setPreviewDirection(null);
                drawStitches();
            }
        });

        btn.setOnMouseClicked(e -> {
            if (renderEngine != null) {
                renderEngine.setPreviewDirection(null);
            }
            switch (direction) {
                case "left"  -> gridManager.duplicateLeft();
                case "right" -> gridManager.duplicateRight();
                case "up"    -> gridManager.duplicateUp();
                case "down"  -> gridManager.duplicateDown();
            }
            drawStitches();
        });
    }

}
