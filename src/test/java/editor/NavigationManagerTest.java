package editor;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class NavigationManagerTest {

    private NavigationManager navManager;

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
    public void setUp() {
        navManager = new NavigationManager();
    }

    @Test
    public void testSetupNavHover() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Label testLabel = new Label("Test Nav");

        Platform.runLater(() -> {
            navManager.setupNavHover(testLabel, true);
            navManager.setActiveNavLabel(testLabel);
            
            // Should apply active style
            assertTrue(testLabel.getStyle().contains("-fx-border-width: 0 0 2 0"), "Active nav should have bottom border");
            assertEquals("ACTIVE", testLabel.getProperties().get("navState").toString(), "Nav state should be ACTIVE");
            
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);
    }

    @Test
    public void testSetupNavHoverIdle() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Label testLabel = new Label("Test Nav Idle");

        Platform.runLater(() -> {
            navManager.setupNavHover(testLabel, false);
            
            // Should apply idle style
            assertTrue(testLabel.getStyle().contains("-fx-border-width: 0"), "Idle nav should not have border width > 0");
            assertEquals("IDLE", testLabel.getProperties().get("navState").toString(), "Nav state should be IDLE");
            
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);
    }

    @Test
    public void testSetupButtonHoverAnimation() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        HBox button = new HBox();

        Platform.runLater(() -> {
            navManager.setupButtonHoverAnimation(button);
            assertNotNull(button.getOnMouseEntered(), "Mouse entered handler should be set");
            assertNotNull(button.getOnMouseExited(), "Mouse exited handler should be set");
            
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);
    }
}
