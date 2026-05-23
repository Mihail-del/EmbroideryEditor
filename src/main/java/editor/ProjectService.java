package editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handles all project persistence operations: saving, loading, and exporting
 * embroidery projects in various formats (JSON, PNG, JPG, GIF).
 */
public class ProjectService {

    private static final String TEMPLATES_DIR = "src/main/resources/templates";

    /**
     * Auto-saves the current project to the internal templates directory.
     *
     * @param projectName the name of the project (used as filename)
     * @param gridManager the grid manager holding stitch data
     */
    public void saveProject(String projectName, GridManager gridManager) {
        String name = (projectName != null && !projectName.trim().isEmpty())
                ? projectName.trim()
                : "project";

        String json = buildProjectJson(name, gridManager);

        try {
            File dir = new File(TEMPLATES_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, name + ".json");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json);
            }
            System.out.println("Auto-saved project to: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the project as a JSON file via a FileChooser dialog.
     *
     * @param projectName the default file name
     * @param gridManager the grid manager holding stitch data
     * @param ownerWindow the parent window for the FileChooser dialog
     */
    public void saveProjectAsJson(String projectName, GridManager gridManager, Window ownerWindow) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Project JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        String name = (projectName != null && !projectName.trim().isEmpty())
                ? projectName.trim()
                : "untitled";
        fileChooser.setInitialFileName(name + ".json");

        File file = fileChooser.showSaveDialog(ownerWindow);
        if (file != null) {
            String json = buildProjectJson(name, gridManager);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json);
                System.out.println("Manually saved project JSON to: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Exports a snapshot image to a file in the specified format.
     *
     * @param snapshot    the WritableImage snapshot (created by the UI layer)
     * @param format      image format: "png", "jpg", or "gif"
     * @param file        the target file to write to
     */
    public void exportImage(WritableImage snapshot, String format, File file) {
        if (snapshot == null || file == null) return;

        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

        // For formats that don't support alpha, composite onto a white background
        if (format.equalsIgnoreCase("jpg") || format.equalsIgnoreCase("jpeg")) {
            BufferedImage jpgImage = new BufferedImage(
                    bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = jpgImage.createGraphics();
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, jpgImage.getWidth(), jpgImage.getHeight());
            g2d.drawImage(bufferedImage, 0, 0, null);
            g2d.dispose();
            bufferedImage = jpgImage;
        }

        try {
            ImageIO.write(bufferedImage, format, file);
            System.out.println("Exported Image to: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens a FileChooser dialog for the user to select an image export location.
     *
     * @param format      the image format extension (e.g. "png")
     * @param projectName the default file name
     * @param ownerWindow the parent window for the dialog
     * @return the selected File, or null if cancelled
     */
    public File chooseImageExportFile(String format, String projectName, Window ownerWindow) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Image as " + format.toUpperCase());
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(format.toUpperCase() + " Image", "*." + format));

        String name = (projectName != null && !projectName.trim().isEmpty())
                ? projectName.trim()
                : "untitled";
        fileChooser.setInitialFileName(name + "." + format);

        return fileChooser.showSaveDialog(ownerWindow);
    }

    /**
     * Opens a FileChooser dialog to select a JSON project file.
     *
     * @param ownerWindow the parent window for the dialog
     * @return the selected File, or null if cancelled
     */
    public File chooseProjectFile(Window ownerWindow) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Project");
        File initialDir = new File(TEMPLATES_DIR);
        if (initialDir.exists()) {
            fileChooser.setInitialDirectory(initialDir);
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        return fileChooser.showOpenDialog(ownerWindow);
    }

    /**
     * Loads project data from a JSON file.
     *
     * @param file the JSON file to load
     * @return a ProjectData object, or null if loading fails
     */
    public ProjectData loadProjectFromFile(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            Gson gson = new Gson();
            @SuppressWarnings("unchecked")
            Map<String, Object> projectData = gson.fromJson(content, Map.class);

            int gridSize = 32;
            if (projectData.containsKey("gridSize")) {
                gridSize = ((Double) projectData.get("gridSize")).intValue();
            }

            String name = file.getName();
            if (name.endsWith(".json")) {
                name = name.substring(0, name.length() - 5);
            }

            List<ProjectData.StitchData> stitchList = new ArrayList<>();
            if (projectData.containsKey("stitches")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> stitches = (List<Map<String, Object>>) projectData.get("stitches");
                for (Map<String, Object> stitch : stitches) {
                    int r = ((Double) stitch.get("row")).intValue();
                    int c = ((Double) stitch.get("col")).intValue();
                    String colorStr = (String) stitch.get("color");
                    Color color = parseRgba(colorStr);
                    stitchList.add(new ProjectData.StitchData(r, c, color));
                }
            }

            System.out.println("Loaded project from: " + file.getAbsolutePath());
            return new ProjectData(gridSize, name, stitchList);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Utility methods ──────────────────────────────────────────────

    /**
     * Converts a JavaFX Color to an RGBA CSS string: rgba(r, g, b, a).
     */
    public static String toRgba(Color color) {
        return String.format(Locale.US, "rgba(%d, %d, %d, %.3f)",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255),
                color.getOpacity());
    }

    /**
     * Parses an RGBA CSS string or hex color string into a JavaFX Color.
     *
     * @param rgba the color string to parse
     * @return the parsed Color, or Color.TRANSPARENT if parsing fails
     */
    public static Color parseRgba(String rgba) {
        if (rgba == null || rgba.isEmpty()) return Color.TRANSPARENT;
        if (rgba.startsWith("rgba(")) {
            String inner = rgba.substring(5, rgba.length() - 1);
            String[] parts = inner.split(",");
            if (parts.length >= 4) {
                try {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    double a = Double.parseDouble(parts[3].trim());
                    return Color.rgb(r, g, b, a);
                } catch (NumberFormatException e) {
                    // Fallback to web
                }
            }
        }
        try {
            return Color.web(rgba);
        } catch (Exception e) {
            return Color.TRANSPARENT;
        }
    }

    // ── Private helpers ──────────────────────────────────────────────

    private String buildProjectJson(String projectName, GridManager gridManager) {
        Map<String, Object> data = new HashMap<>();
        data.put("gridSize", gridManager.getGridSize());
        data.put("stitches", gridManager.getStitchesAsList(ProjectService::toRgba));

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(data);
    }
}
