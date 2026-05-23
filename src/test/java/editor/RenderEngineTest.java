package editor;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RenderEngineTest {

    private GridManager gridManager;
    private Canvas gridCanvas;
    private Canvas stitchCanvas;
    private RenderEngine renderEngine;

    @BeforeAll
    public static void initJavaFX() throws InterruptedException {
        Thread fxThread = new Thread(() -> {
            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException e) {
            }
        });
        fxThread.setDaemon(true);
        fxThread.start();
        fxThread.join(1000);
    }

    @BeforeEach
    public void setUp() {
        Platform.runLater(() -> {
            gridManager = new GridManager();
            gridCanvas = new Canvas(400, 400);
            stitchCanvas = new Canvas(400, 400);
            renderEngine = new RenderEngine(gridCanvas, stitchCanvas, gridManager);
        });

        waitForFxEvents();
    }

    @Test
    public void testRenderEngineInitialization() {
        assertNotNull(renderEngine, "Графический движок должен быть успешно создан");
    }

    @Test
    public void testDrawGridDoesNotThrow() {
        assertDoesNotThrow(() -> {
            Platform.runLater(() -> renderEngine.drawGrid(true, true));
            waitForFxEvents();
        });
    }

    @Test
    public void testDrawStitchesDoesNotThrow() {
        assertDoesNotThrow(() -> {
            Platform.runLater(() -> {
                gridManager.setStitchColor(0, 0, javafx.scene.paint.Color.RED);
                renderEngine.drawStitches();
            });
            waitForFxEvents();
        });
    }

    /**
     * Helper method for synchronizing the JUnit thread with the JavaFX UI thread
     */
    private void waitForFxEvents() {
        try {
            java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(0);
            Platform.runLater(semaphore::release);
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}