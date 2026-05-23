package editor;

import javafx.scene.paint.Color;
import java.util.List;

/**
 * Immutable data model representing a loaded embroidery project.
 * Used to transfer project data between ProjectService and MainController.
 */
public class ProjectData {

    /**
     * Represents a single stitch entry with its grid position and color.
     */
    public static class StitchData {
        private final int row;
        private final int col;
        private final Color color;

        public StitchData(int row, int col, Color color) {
            this.row = row;
            this.col = col;
            this.color = color;
        }

        public int row() { return row; }
        public int col() { return col; }
        public Color color() { return color; }
    }

    private final int gridSize;
    private final String projectName;
    private final List<StitchData> stitches;

    public ProjectData(int gridSize, String projectName, List<StitchData> stitches) {
        this.gridSize = gridSize;
        this.projectName = projectName;
        this.stitches = stitches;
    }

    public int getGridSize() {
        return gridSize;
    }

    public String getProjectName() {
        return projectName;
    }

    public List<StitchData> getStitches() {
        return stitches;
    }
}
