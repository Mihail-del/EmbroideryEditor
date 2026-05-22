package editor;

import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridManager {
    static final int GRID_MIN = 8;
    static final int GRID_MAX = 96;
    public static final int GRID_STEP = 8;

    private int gridSize = 32;
    private Color[][] stitchColors;

    public GridManager() {
        resetStitches();
    }

    public int getGridSize() {
        return gridSize;
    }

    public void resetStitches() {
        this.stitchColors = new Color[gridSize][gridSize];
    }

    /**
     * Resizes the grid with bounds checking (GRID_MIN <= size <= GRID_MAX)
     * @param delta is the resizing step (e.g., +8 or -8)
     * @return true if the size has changed, false otherwise
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
     * Applies changes to the cell taking into account active symmetry modes.
     * Covers both color drawing and erasing (if targetColor == null).
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
     * Export current stitches to a List of structures for subsequent saving as JSON.
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
}