package editor;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class StitchInteractionHandlerTest {

    private StitchInteractionHandler handler;
    private GridManager gridManager;
    private boolean menuVisible;
    private boolean verticalSymmetry;
    private boolean horizontalSymmetry;
    private Color currentColor;
    private boolean drawCallbackFired;

    @BeforeAll
    public static void initJavaFX() throws InterruptedException {
        Thread fxThread = new Thread(() -> {
            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException e) {
                // Ignore if already running
            }
        });
        fxThread.setDaemon(true);
        fxThread.start();
        fxThread.join(1000);
    }

    @BeforeEach
    public void setUp() {
        gridManager = new GridManager();
        menuVisible = false;
        verticalSymmetry = false;
        horizontalSymmetry = false;
        currentColor = Color.RED;
        drawCallbackFired = false;

        handler = new StitchInteractionHandler(
                gridManager,
                () -> currentColor,
                () -> verticalSymmetry,
                () -> horizontalSymmetry,
                () -> menuVisible,
                () -> drawCallbackFired = true
        );
    }

    @Test
    public void testToggleEraser() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Label eraserBtn = new Label("Eraser");

        Platform.runLater(() -> {
            assertFalse(handler.isEraserActive());
            
            handler.toggleEraser(eraserBtn);
            assertTrue(handler.isEraserActive(), "Eraser should be active");
            assertTrue(eraserBtn.getStyleClass().contains("eraser-active"), "Should have active class");
            
            handler.toggleEraser(eraserBtn);
            assertFalse(handler.isEraserActive(), "Eraser should be inactive");
            assertFalse(eraserBtn.getStyleClass().contains("eraser-active"), "Should not have active class");
            
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);
    }

    @Test
    public void testClearCanvas() {
        gridManager.setStitchColor(0, 0, Color.BLUE);
        assertFalse(gridManager.isCanvasClear());

        handler.clearCanvas();

        assertTrue(gridManager.isCanvasClear(), "Grid should be clear");
        assertTrue(drawCallbackFired, "Draw callback should be fired after clear");
    }

    @Test
    public void testSetupStitchLayerAndClick() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Canvas stitchCanvas = new Canvas();
        StackPane mainView = new StackPane();

        Platform.runLater(() -> {
            javafx.scene.Scene scene = new javafx.scene.Scene(mainView, 100, 100);
            handler.setupStitchLayer(stitchCanvas, mainView);
            mainView.getChildren().add(stitchCanvas);
            mainView.applyCss();
            mainView.layout();
            
            // At this point mainView and stitchCanvas are 100x100
            // With GRID_PADDING = 12, inner size = 76. cellSize = 76/32 = 2.375
            // Click at x=13, y=13 corresponds to row 0, col 0
            
            javafx.scene.input.MouseEvent clickEvent = new javafx.scene.input.MouseEvent(
                    javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                    13, 13, 13, 13, javafx.scene.input.MouseButton.PRIMARY, 1,
                    true, true, true, true, true, true, true, true, true, true, null
            );
            stitchCanvas.fireEvent(clickEvent);
            
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);

        assertEquals(Color.RED, gridManager.getStitchColor(0, 0), "Stitch should be placed at 0,0");
        assertTrue(drawCallbackFired, "Draw callback should be fired after stitch placement");
    }

    @Test
    public void testClickIgnoredWhenMenuVisible() throws Exception {
        menuVisible = true; // Set menu to visible
        CountDownLatch latch = new CountDownLatch(1);
        Canvas stitchCanvas = new Canvas(100, 100);
        StackPane mainView = new StackPane();

        Platform.runLater(() -> {
            handler.setupStitchLayer(stitchCanvas, mainView);
            
            javafx.scene.input.MouseEvent clickEvent = new javafx.scene.input.MouseEvent(
                    javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                    13, 13, 13, 13, javafx.scene.input.MouseButton.PRIMARY, 1,
                    true, true, true, true, true, true, true, true, true, true, null
            );
            stitchCanvas.fireEvent(clickEvent);
            
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);

        assertNull(gridManager.getStitchColor(0, 0), "No stitch should be placed when menu is visible");
    }
}
