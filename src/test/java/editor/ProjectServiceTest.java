package editor;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectServiceTest {

    private ProjectService projectService;
    private GridManager gridManager;
    private File tempDir;

    @BeforeAll
    public static void initJavaFX() throws InterruptedException {
        Thread fxThread = new Thread(() -> {
            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException e) {
                // JavaFX is already running
            }
        });
        fxThread.setDaemon(true);
        fxThread.start();
        fxThread.join(1000);
    }

    @BeforeEach
    public void setUp() throws Exception {
        projectService = new ProjectService();
        gridManager = new GridManager();
        
        // Create a temporary directory for test files
        tempDir = Files.createTempDirectory("project_service_test").toFile();
    }

    @AfterEach
    public void tearDown() {
        if (tempDir != null && tempDir.exists()) {
            for (File file : tempDir.listFiles()) {
                file.delete();
            }
            tempDir.delete();
        }
    }

    @Test
    public void testToRgbaConversion() {
        Color color = Color.rgb(255, 0, 0, 1.0);
        String rgba = ProjectService.toRgba(color);
        assertEquals("rgba(255, 0, 0, 1.000)", rgba, "Opaque red conversion failed");

        Color semiTransparentBlue = Color.rgb(0, 0, 255, 0.5);
        String rgbaSemi = ProjectService.toRgba(semiTransparentBlue);
        assertEquals("rgba(0, 0, 255, 0.500)", rgbaSemi, "Semi-transparent blue conversion failed");
    }

    @Test
    public void testParseRgbaValid() {
        Color parsed = ProjectService.parseRgba("rgba(255, 0, 0, 1.0)");
        assertEquals(Color.rgb(255, 0, 0, 1.0), parsed, "Parsed red color does not match");
        
        Color parsedHex = ProjectService.parseRgba("#00FF00");
        assertEquals(Color.web("#00FF00"), parsedHex, "Parsed hex green does not match");
    }

    @Test
    public void testParseRgbaInvalid() {
        Color parsedInvalidFormat = ProjectService.parseRgba("invalid-color");
        assertEquals(Color.TRANSPARENT, parsedInvalidFormat, "Invalid color should fallback to transparent");

        Color parsedNull = ProjectService.parseRgba(null);
        assertEquals(Color.TRANSPARENT, parsedNull, "Null color string should yield transparent");
        
        Color parsedEmpty = ProjectService.parseRgba("");
        assertEquals(Color.TRANSPARENT, parsedEmpty, "Empty color string should yield transparent");
    }

    @Test
    public void testLoadProjectFromInvalidFile() {
        File fakeFile = new File(tempDir, "non_existent.json");
        ProjectData data = projectService.loadProjectFromFile(fakeFile);
        assertNull(data, "Loading a non-existent file should return null");
    }

    @Test
    public void testSaveAndLoadProjectIntegration() throws Exception {
        // Setup GridManager state
        gridManager.setGridSize(16);
        gridManager.setStitchColor(0, 0, Color.RED);
        gridManager.setStitchColor(15, 15, Color.BLUE);
        
        String projectName = "integration_test_" + UUID.randomUUID().toString();
        
        // Use a temporary file path
        File saveFile = new File(tempDir, projectName + ".json");
        
        // Manually trigger the private save mechanism using reflection to test the JSON string building and reading
        // We will simulate what `saveProject` does to a specific file
        String json = buildProjectJsonReflection(projectName, gridManager);
        Files.writeString(saveFile.toPath(), json);

        // Load it back
        ProjectData loadedData = projectService.loadProjectFromFile(saveFile);
        
        assertNotNull(loadedData, "Loaded data should not be null");
        assertEquals(16, loadedData.getGridSize(), "Grid size should be 16");
        assertEquals(projectName, loadedData.getProjectName(), "Project name should match");
        
        assertEquals(2, loadedData.getStitches().size(), "There should be exactly 2 stitches saved");
        
        boolean foundRed = false;
        boolean foundBlue = false;
        
        for (ProjectData.StitchData stitch : loadedData.getStitches()) {
            if (stitch.row() == 0 && stitch.col() == 0 && stitch.color().equals(Color.RED)) {
                foundRed = true;
            }
            if (stitch.row() == 15 && stitch.col() == 15 && stitch.color().equals(Color.BLUE)) {
                foundBlue = true;
            }
        }
        
        assertTrue(foundRed, "Top-left red stitch should be loaded");
        assertTrue(foundBlue, "Bottom-right blue stitch should be loaded");
    }
    
    // Helper to call private method buildProjectJson using reflection
    private String buildProjectJsonReflection(String projectName, GridManager manager) throws Exception {
        java.lang.reflect.Method method = ProjectService.class.getDeclaredMethod("buildProjectJson", String.class, GridManager.class);
        method.setAccessible(true);
        return (String) method.invoke(projectService, projectName, manager);
    }
}
