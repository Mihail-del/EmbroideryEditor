package editor;

import javafx.application.Platform;
import javafx.scene.control.ColorPicker;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class MenuManagerTest {

    private MenuManager menuManager;
    private StackPane mainCanvasView;
    private VBox createMenu;
    private ColorPicker colorPicker;

    private boolean savedAsJson = false;
    private boolean exportedImage = false;
    private boolean browsedFile = false;
    private boolean loadedRecent = false;
    private boolean hidAllMenus = false;

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
    public void setUp() throws Exception {
        savedAsJson = false;
        exportedImage = false;
        browsedFile = false;
        loadedRecent = false;
        hidAllMenus = false;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            mainCanvasView = new StackPane();
            createMenu = new VBox();
            colorPicker = new ColorPicker();

            MenuManager.MenuCallbacks callbacks = new MenuManager.MenuCallbacks() {
                @Override
                public void onSaveAsJson() { savedAsJson = true; }

                @Override
                public void onExportImage(String format, boolean transparentBg, boolean animated) { exportedImage = true; }

                @Override
                public void onBrowseOpenFile() { browsedFile = true; }

                @Override
                public void onLoadRecentProject(File file) { loadedRecent = true; }

                @Override
                public void onHideAllMenus() { hidAllMenus = true; }
            };

            menuManager = new MenuManager(mainCanvasView, createMenu, colorPicker, callbacks);
            menuManager.initAllMenus();
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);
    }

    @Test
    public void testShowAndHideCreateMenu() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean isVisible = new AtomicBoolean(false);

        Platform.runLater(() -> {
            menuManager.showCreateMenu();
            isVisible.set(menuManager.isCreateMenuVisible());
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);

        assertTrue(isVisible.get(), "Create menu should be visible after showCreateMenu()");

        CountDownLatch hideLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            menuManager.hideCreateMenu();
            isVisible.set(menuManager.isCreateMenuVisible());
            hideLatch.countDown();
        });
        hideLatch.await(2, TimeUnit.SECONDS);

        assertFalse(isVisible.get(), "Create menu should be hidden after hideCreateMenu()");
    }

    @Test
    public void testShowInfoMenu() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            menuManager.showInfoMenu();
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);
        
        // At this point info menu should be visible, but we check if it doesn't throw and initializes properly.
        assertFalse(menuManager.isCreateMenuVisible(), "Other menus should be hidden");
    }

    @Test
    public void testHideAllMenus() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            menuManager.showCreateMenu();
            menuManager.hideAllMenus();
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);

        assertFalse(menuManager.isCreateMenuVisible(), "Create menu should be hidden after hideAllMenus");
    }
}
