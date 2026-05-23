package editor;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectDataTest {

    @Test
    public void testStitchDataCreation() {
        ProjectData.StitchData stitch = new ProjectData.StitchData(5, 10, Color.RED);
        assertEquals(5, stitch.row(), "Row should match");
        assertEquals(10, stitch.col(), "Col should match");
        assertEquals(Color.RED, stitch.color(), "Color should match");
    }

    @Test
    public void testProjectDataCreation() {
        List<ProjectData.StitchData> stitches = Arrays.asList(
                new ProjectData.StitchData(0, 0, Color.BLUE),
                new ProjectData.StitchData(1, 1, Color.GREEN)
        );

        ProjectData projectData = new ProjectData(32, "TestProject", stitches);

        assertEquals(32, projectData.getGridSize(), "Grid size should match");
        assertEquals("TestProject", projectData.getProjectName(), "Project name should match");
        assertEquals(2, projectData.getStitches().size(), "Should contain 2 stitches");
        
        ProjectData.StitchData firstStitch = projectData.getStitches().get(0);
        assertEquals(0, firstStitch.row());
        assertEquals(0, firstStitch.col());
        assertEquals(Color.BLUE, firstStitch.color());
    }

    @Test
    public void testProjectDataWithEmptyStitches() {
        ProjectData projectData = new ProjectData(16, "EmptyProject", List.of());
        
        assertEquals(16, projectData.getGridSize());
        assertEquals("EmptyProject", projectData.getProjectName());
        assertTrue(projectData.getStitches().isEmpty(), "Stitches list should be empty");
    }
}
