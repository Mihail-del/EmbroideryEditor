package editor;

import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the state, colors, bounds, and symmetry processing of the embroidery grid.
 */
public class GridManager {
    public static final int GRID_MIN = 8;
    public static final int GRID_MAX = 96;
    public static final int GRID_STEP = 8;

    private int gridSize = 32;
    private Color[][] stitchColors;

    public GridManager() {
        resetStitches();
    }

    public int getGridSize() {
        return gridSize;
    }

    /**
     * Resets the 2D matrix structure to match the current grid size configuration.
     */
    public void resetStitches() {
        this.stitchColors = new Color[gridSize][gridSize];
    }

    /**
     * Updates the grid size incrementally with strict boundary enforcement.
     * @param delta value to change the size by (+8 or -8)
     * @return true if changed successfully, false if hitting boundaries
     */
    public boolean updateGridSize(int delta) {
        int next = Math.max(GRID_MIN, Math.min(GRID_MAX, gridSize + delta));
        if (next == gridSize) {
            return false;
        }
        gridSize = next;
        resetStitches();
        return true;
    }

    /**
     * Increases the grid size by one standard step configuration block.
     * @return true if successful
     */
    public boolean incrementGridSize() {
        return updateGridSize(GRID_STEP);
    }

    /**
     * Decreases the grid size by one standard step configuration block.
     * @return true if successful
     */
    public boolean decrementGridSize() {
        return updateGridSize(-GRID_STEP);
    }

    /**
     * Updates grid layout to specific absolute dimension sizes.
     * @param size requested matrix width/height bounds
     */
    public void setGridSize(int size) {
        this.gridSize = Math.max(GRID_MIN, Math.min(GRID_MAX, size));
        resetStitches();
    }

    public Color getStitchColor(int row, int col) {
        if (isValidCoordinate(row, col)) {
            return stitchColors[row][col];
        }
        return null;
    }

    public void setStitchColor(int row, int col, Color color) {
        if (isValidCoordinate(row, col)) {
            stitchColors[row][col] = color;
        }
    }

    public boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < gridSize && col >= 0 && col < gridSize;
    }

    /**
     * Scans the board state structure to verify if any visual cell is filled.
     * @return true if completely white space layout context
     */
    public boolean isCanvasClear() {
        if (stitchColors == null) return true;
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                if (stitchColors[r][c] != null) return false;
            }
        }
        return true;
    }

    /**
     * Commits pixel structural colors mapping using specific mirroring symmetry modes.
     */
    public void applyStitchWithSymmetry(int row, int col, Color targetColor, boolean verticalSym, boolean horizontalSym) {
        if (!isValidCoordinate(row, col)) {
            return;
        }

        stitchColors[row][col] = targetColor;

        if (verticalSym) {
            stitchColors[row][(gridSize - 1) - col] = targetColor;
        }
        if (horizontalSym) {
            stitchColors[(gridSize - 1) - row][col] = targetColor;
        }
        if (verticalSym && horizontalSym) {
            stitchColors[(gridSize - 1) - row][(gridSize - 1) - col] = targetColor;
        }
    }

    /**
     * Extracts active values to key-value maps compatible with standard JSON formats.
     */
    public List<Map<String, Object>> getStitchesAsList(java.util.function.Function<Color, String> colorSerializer) {
        List<Map<String, Object>> stitches = new ArrayList<>();
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                if (stitchColors[r][c] != null) {
                    Map<String, Object> stitch = new HashMap<>();
                    stitch.put("row", r);
                    stitch.put("col", c);
                    stitch.put("color", colorSerializer.apply(stitchColors[r][c]));
                    stitches.add(stitch);
                }
            }
        }
        return stitches;
    }

    /**
     * Duplicates the right half of the canvas to the left half.
     */
    public void duplicateLeft() {
        int half = gridSize / 2;
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < half; c++) {
                stitchColors[r][c] = stitchColors[r][c + half];
            }
        }
    }

    /**
     * Duplicates the left half of the canvas to the right half.
     */
    public void duplicateRight() {
        int half = gridSize / 2;
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < half; c++) {
                stitchColors[r][c + half] = stitchColors[r][c];
            }
        }
    }

    /**
     * Duplicates the bottom half of the canvas to the top half.
     */
    public void duplicateUp() {
        int half = gridSize / 2;
        for (int r = 0; r < half; r++) {
            for (int c = 0; c < gridSize; c++) {
                stitchColors[r][c] = stitchColors[r + half][c];
            }
        }
    }

    /**
     * Duplicates the top half of the canvas to the bottom half.
     */
    public void duplicateDown() {
        int half = gridSize / 2;
        for (int r = 0; r < half; r++) {
            for (int c = 0; c < gridSize; c++) {
                stitchColors[r + half][c] = stitchColors[r][c];
            }
        }
    }
}