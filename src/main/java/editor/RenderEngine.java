package editor;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for all canvas rendering in the application, including drawing the grid and stitches.
 * It manages the rendering logic, image tinting for stitches, and caching of tinted images for performance optimization.
 * The grid is drawn with a dot pattern and optional symmetry lines, while stitches are rendered as tinted cross images based on their assigned colors in the GridManager.
 */
public class RenderEngine {
    private final Canvas gridCanvas;
    private final Canvas stitchCanvas;

    private final GridManager gridManager;

    private Image crossImage;
    private final Map<String, Image> tintCache = new HashMap<>();

    private String previewDirection = null;

    private static final double DOT_RADIUS = 1.5;
    private static final double GRID_PADDING = 12.0;
    private static final Color GRID_DOT_COLOR = Color.web("#97958C", 0.6);

    public RenderEngine(Canvas gridCanvas, Canvas stitchCanvas, GridManager gridManager) {
        this.gridCanvas = gridCanvas;
        this.stitchCanvas = stitchCanvas;
        this.gridManager = gridManager;
        loadCrossImage();
    }

    public void setPreviewDirection(String direction) {
        this.previewDirection = direction;
    }

    public String getPreviewDirection() {
        return previewDirection;
    }

    private void loadCrossImage() {
        var url = getClass().getResource("/images/cross_sample.png");
        if (url != null) {
            crossImage = new Image(url.toExternalForm());
            tintCache.clear();
        }
    }

    public void drawGrid(boolean isVerticalSymmetryActive, boolean isHorizontalSymmetryActive) {
        if (gridCanvas == null) return;

        double width = gridCanvas.getWidth();
        double height = gridCanvas.getHeight();
        if (width <= 0 || height <= 0) return;

        GraphicsContext gc = gridCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        gc.setFill(GRID_DOT_COLOR);

        double innerWidth = Math.max(0, width - (GRID_PADDING * 2));
        double innerHeight = Math.max(0, height - (GRID_PADDING * 2));
        if (innerWidth <= 0 || innerHeight <= 0) return;

        int gridSize = gridManager.getGridSize();
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
            gc.setStroke(Color.web("#D97757", 0.8));
            gc.setLineWidth(2.0);
            gc.setLineDashes(8, 8);

            if (isVerticalSymmetryActive) {
                double exactMiddleX = GRID_PADDING + (gridTotalWidth / 2.0);
                gc.strokeLine(exactMiddleX, GRID_PADDING, exactMiddleX, GRID_PADDING + gridTotalHeight);
            }

            if (isHorizontalSymmetryActive) {
                double exactMiddleY = GRID_PADDING + (gridTotalHeight / 2.0);
                gc.strokeLine(GRID_PADDING, exactMiddleY, gridTotalWidth + GRID_PADDING, exactMiddleY);
            }

            gc.setLineDashes((double[]) null);
        }
    }

    public void drawStitches() {
        if (stitchCanvas == null) return;

        double width = stitchCanvas.getWidth();
        double height = stitchCanvas.getHeight();
        if (width <= 0 || height <= 0) return;

        GraphicsContext gc = stitchCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        if (crossImage == null || crossImage.isError()) return;

        double innerWidth = Math.max(0, width - (GRID_PADDING * 2));
        double innerHeight = Math.max(0, height - (GRID_PADDING * 2));
        if (innerWidth <= 0 || innerHeight <= 0) return;

        int gridSize = gridManager.getGridSize();
        double cellSize = Math.min(innerWidth, innerHeight) / gridSize;
        double imageSize = Math.max(1, cellSize * 0.8);

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                Color color = gridManager.getStitchColor(row, col);

                if (previewDirection != null) {
                    int half = gridSize / 2;
                    if (previewDirection.equals("left") && col < half) {
                        Color srcColor = gridManager.getStitchColor(row, col + half);
                        color = (srcColor != null) ? new Color(srcColor.getRed(), srcColor.getGreen(), srcColor.getBlue(), 0.5) : null;
                    } else if (previewDirection.equals("right") && col >= half) {
                        Color srcColor = gridManager.getStitchColor(row, col - half);
                        color = (srcColor != null) ? new Color(srcColor.getRed(), srcColor.getGreen(), srcColor.getBlue(), 0.5) : null;
                    } else if (previewDirection.equals("up") && row < half) {
                        Color srcColor = gridManager.getStitchColor(row + half, col);
                        color = (srcColor != null) ? new Color(srcColor.getRed(), srcColor.getGreen(), srcColor.getBlue(), 0.5) : null;
                    } else if (previewDirection.equals("down") && row >= half) {
                        Color srcColor = gridManager.getStitchColor(row - half, col);
                        color = (srcColor != null) ? new Color(srcColor.getRed(), srcColor.getGreen(), srcColor.getBlue(), 0.5) : null;
                    }
                }

                if (color == null) continue;

                Image paintImage = getTintedImage(color);
                double x = GRID_PADDING + (col * cellSize) + (cellSize - imageSize) / 2.0;
                double y = GRID_PADDING + (row * cellSize) + (cellSize - imageSize) / 2.0;
                gc.drawImage(paintImage, x, y, imageSize, imageSize);
            }
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
        if (cached != null) return cached;

        int width = (int) crossImage.getWidth();
        int height = (int) crossImage.getHeight();
        if (width <= 0 || height <= 0) return crossImage;

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
                writer.setColor(x, y, new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha * color.getOpacity()));
            }
        }

        tintCache.put(key, tinted);
        return tinted;
    }
}