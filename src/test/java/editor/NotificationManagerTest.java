package editor;

import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationManagerTest {

    private NotificationManager notificationManager;
    private Label testLabel;

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
        testLabel = new Label("✓ Autosaved");
        testLabel.setVisible(false);
        notificationManager = new NotificationManager(testLabel);
    }

    @Test
    public void testShowAutoSaveNotification() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            assertFalse(testLabel.isVisible(), "Label should initially be invisible");
            notificationManager.showAutoSaveNotification();
            assertTrue(testLabel.isVisible(), "Label should become visible");
            assertEquals(0.0, testLabel.getOpacity(), 0.01, "Label should start transparent before animation plays out");
            assertEquals(-10.0, testLabel.getTranslateY(), 0.01, "Label should start offset vertically");
            
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }
}
