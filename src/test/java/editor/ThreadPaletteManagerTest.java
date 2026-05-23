package editor;

import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadPaletteManagerTest {

    private ThreadPaletteManager paletteManager;
    private StackPane mainLayout;

    @BeforeAll
    public static void initJavaFX() throws InterruptedException {
        Thread fxThread = new Thread(() -> {
            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException e) {
                // JavaFX already started
            }
        });
        fxThread.setDaemon(true);
        fxThread.start();
        fxThread.join(1000);
    }

    @BeforeEach
    public void setUp() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            mainLayout = new StackPane();
            paletteManager = new ThreadPaletteManager(mainLayout);
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);
    }

    @Test
    public void testInitColorPicker() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            paletteManager.initColorPicker();
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);

        assertNotNull(paletteManager.getColorPicker(), "Color picker should be initialized");
        assertTrue(mainLayout.getChildren().contains(paletteManager.getColorPicker()), "Main layout should contain color picker");
    }

    @Test
    public void testSetupColorCircleAndSetActive() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        StackPane circlePane = new StackPane();
        
        Platform.runLater(() -> {
            paletteManager.setupColorCircle(circlePane, Color.BLUE);
            paletteManager.setActiveCircle(circlePane);
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);

        assertEquals(Color.BLUE, paletteManager.getCurrentThreadColor(), "Active color should be updated to BLUE");
        assertTrue(circlePane.getStyleClass().contains("color-circle-active"), "Active circle should have 'color-circle-active' class");
    }

    @Test
    public void testHideMenusCallback() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean callbackTriggered = new AtomicBoolean(false);
        StackPane circlePane = new StackPane();

        Platform.runLater(() -> {
            paletteManager.initColorPicker();
            paletteManager.setupColorCircle(circlePane, Color.TRANSPARENT); // Empty circle
            paletteManager.setHideAllMenusCallback(() -> callbackTriggered.set(true));
            
            // Simulating internal click behavior for empty circle which triggers openColorPicker
            // Since we can't easily fire mouse events in headless without TestFX, we call active method and check callback.
            // Wait, we can fire event: circlePane.fireEvent(new javafx.scene.input.MouseEvent(...))
            // But it's easier to just use the public method if we can, or rely on mouse event generation.
            
            javafx.scene.input.MouseEvent clickEvent = new javafx.scene.input.MouseEvent(
                    javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                    0, 0, 0, 0, javafx.scene.input.MouseButton.PRIMARY, 1,
                    true, true, true, true, true, true, true, true, true, true, null
            );
            circlePane.fireEvent(clickEvent);
            
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);

        assertTrue(callbackTriggered.get(), "Hide menus callback should be triggered on empty circle click");
        assertTrue(paletteManager.getColorPicker().isShowing() || paletteManager.getColorPicker().isVisible(), "Color picker should be shown");
    }
}
