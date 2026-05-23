package editor;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Handles user interaction with the stitch canvas: click-to-place stitches,
 * eraser mode toggling, canvas clearing, and stitch layer setup.
 *
 * <p>This class follows the Single Responsibility Principle by isolating
 * all stitch placement and eraser logic from the main controller.</p>
 */
public class StitchInteractionHandler {

    private static final double GRID_PADDING = 12.0;

    private final GridManager gridManager;
    private final Supplier<Color> currentColorSupplier;
    private final BooleanSupplier verticalSymmetrySupplier;
    private final BooleanSupplier horizontalSymmetrySupplier;
    private final BooleanSupplier createMenuVisibleSupplier;
    private final Runnable drawStitchesCallback;

    private boolean isEraserActive = false;

    /**
     * Creates a new StitchInteractionHandler.
     *
     * @param gridManager                 the grid manager holding stitch state
     * @param currentColorSupplier        supplier for the current thread color
     * @param verticalSymmetrySupplier    supplier for vertical symmetry state
     * @param horizontalSymmetrySupplier  supplier for horizontal symmetry state
     * @param createMenuVisibleSupplier   supplier that returns true if the create menu is open
     * @param drawStitchesCallback        callback to redraw stitches after changes
     */
    public StitchInteractionHandler(
            GridManager gridManager,
            Supplier<Color> currentColorSupplier,
            BooleanSupplier verticalSymmetrySupplier,
            BooleanSupplier horizontalSymmetrySupplier,
            BooleanSupplier createMenuVisibleSupplier,
            Runnable drawStitchesCallback) {
        this.gridManager = gridManager;
        this.currentColorSupplier = currentColorSupplier;
        this.verticalSymmetrySupplier = verticalSymmetrySupplier;
        this.horizontalSymmetrySupplier = horizontalSymmetrySupplier;
        this.createMenuVisibleSupplier = createMenuVisibleSupplier;
        this.drawStitchesCallback = drawStitchesCallback;
    }

    // ── Stitch Layer Setup ───────────────────────────────────────────

    /**
     * Binds the stitch canvas size to the main canvas view and sets up the click handler.
     *
     * @param stitchCanvas   the canvas for rendering stitches
     * @param mainCanvasView the parent pane that determines canvas size
     */
    public void setupStitchLayer(Canvas stitchCanvas, StackPane mainCanvasView) {
        if (stitchCanvas == null || mainCanvasView == null) {
            return;
        }
        stitchCanvas.widthProperty().bind(mainCanvasView.widthProperty());
        stitchCanvas.heightProperty().bind(mainCanvasView.heightProperty());
        stitchCanvas.widthProperty().addListener((obs, oldVal, newVal) -> drawStitchesCallback.run());
        stitchCanvas.heightProperty().addListener((obs, oldVal, newVal) -> drawStitchesCallback.run());
        stitchCanvas.setOnMouseClicked(e -> handleStitchClick(stitchCanvas, e.getX(), e.getY()));
    }

    // ── Stitch Click ─────────────────────────────────────────────────

    private void handleStitchClick(Canvas stitchCanvas, double x, double y) {
        if (createMenuVisibleSupplier.getAsBoolean()) {
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

        boolean vSym = verticalSymmetrySupplier.getAsBoolean();
        boolean hSym = horizontalSymmetrySupplier.getAsBoolean();

        if (isEraserActive) {
            gridManager.applyStitchWithSymmetry(row, col, null, vSym, hSym);
        } else {
            Color threadColor = currentColorSupplier.get();
            if (threadColor == null || threadColor.equals(Color.TRANSPARENT)) {
                return;
            }
            gridManager.applyStitchWithSymmetry(row, col, threadColor, vSym, hSym);
        }
        drawStitchesCallback.run();
    }

    // ── Eraser ───────────────────────────────────────────────────────

    /**
     * Toggles eraser mode and updates the eraser button styling.
     *
     * @param eraserBtn the eraser label button (may be null)
     */
    public void toggleEraser(Label eraserBtn) {
        isEraserActive = !isEraserActive;
        if (eraserBtn != null) {
            if (isEraserActive) {
                eraserBtn.getStyleClass().add("eraser-active");
            } else {
                eraserBtn.getStyleClass().remove("eraser-active");
            }
        }
    }

    /**
     * Returns whether the eraser is currently active.
     */
    public boolean isEraserActive() {
        return isEraserActive;
    }

    // ── Canvas Clear ─────────────────────────────────────────────────

    /**
     * Clears all stitches and redraws.
     */
    public void clearCanvas() {
        gridManager.resetStitches();
        drawStitchesCallback.run();
    }
}
