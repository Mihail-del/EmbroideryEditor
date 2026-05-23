package editor;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GridManagerTest {

    private GridManager gridManager;

    @BeforeEach
    public void setUp() {
        gridManager = new GridManager();
    }

    @Test
    public void testDefaultInitialization() {
        assertEquals(32, gridManager.getGridSize());
        assertTrue(gridManager.isCanvasClear());
    }

    @Test
    public void testUpdateGridSizeWithinBounds() {
        assertTrue(gridManager.incrementGridSize());
        assertEquals(40, gridManager.getGridSize());

        assertTrue(gridManager.decrementGridSize());
        assertEquals(32, gridManager.getGridSize());
    }

    @Test
    public void testGridSizeLimits() {
        gridManager.setGridSize(8);

        assertFalse(gridManager.decrementGridSize());
        assertEquals(8, gridManager.getGridSize());

        gridManager.setGridSize(96);

        assertFalse(gridManager.incrementGridSize());
        assertEquals(96, gridManager.getGridSize());
    }

    @Test
    public void testIsValidCoordinate() {
        int size = gridManager.getGridSize();
        assertTrue(gridManager.isValidCoordinate(0, 0));
        assertTrue(gridManager.isValidCoordinate(size - 1, size - 1));

        assertFalse(gridManager.isValidCoordinate(-1, 0));
        assertFalse(gridManager.isValidCoordinate(0, size));
    }

    @Test
    public void testApplyStitchWithoutSymmetry() {
        Color targetColor = Color.RED;
        gridManager.applyStitchWithSymmetry(5, 5, targetColor, false, false);

        assertEquals(targetColor, gridManager.getStitchColor(5, 5));
        assertFalse(gridManager.isCanvasClear());
    }

    @Test
    public void testApplyStitchWithVerticalSymmetry() {
        Color targetColor = Color.BLUE;
        int size = gridManager.getGridSize();

        gridManager.applyStitchWithSymmetry(2, 5, targetColor, true, false);

        assertEquals(targetColor, gridManager.getStitchColor(2, 5));
        assertEquals(targetColor, gridManager.getStitchColor(2, (size - 1) - 5));
    }

    @Test
    public void testApplyStitchWithFullSymmetry() {
        Color targetColor = Color.GREEN;
        int size = gridManager.getGridSize();

        gridManager.applyStitchWithSymmetry(4, 4, targetColor, true, true);

        assertEquals(targetColor, gridManager.getStitchColor(4, 4));
        assertEquals(targetColor, gridManager.getStitchColor(4, (size - 1) - 4));
        assertEquals(targetColor, gridManager.getStitchColor((size - 1) - 4, 4));
        assertEquals(targetColor, gridManager.getStitchColor((size - 1) - 4, (size - 1) - 4));
    }

    @Test
    public void testGetStitchesAsList() {
        gridManager.setStitchColor(0, 0, Color.BLACK);

        List<Map<String, Object>> list = gridManager.getStitchesAsList(color -> "rgba(0,0,0,1)");

        assertEquals(1, list.size());
        assertEquals(0, list.get(0).get("row"));
        assertEquals(0, list.get(0).get("col"));
        assertEquals("rgba(0,0,0,1)", list.get(0).get("color"));
    }

    @Test
    public void testDuplicateLeft() {
        gridManager.setStitchColor(5, 20, Color.RED);
        gridManager.duplicateLeft();
        assertEquals(Color.RED, gridManager.getStitchColor(5, 4));
        assertEquals(Color.RED, gridManager.getStitchColor(5, 20));
    }

    @Test
    public void testDuplicateRight() {
        gridManager.setStitchColor(5, 4, Color.BLUE);
        gridManager.duplicateRight();
        assertEquals(Color.BLUE, gridManager.getStitchColor(5, 4));
        assertEquals(Color.BLUE, gridManager.getStitchColor(5, 20));
    }

    @Test
    public void testDuplicateUp() {
        gridManager.setStitchColor(20, 5, Color.GREEN);
        gridManager.duplicateUp();
        assertEquals(Color.GREEN, gridManager.getStitchColor(4, 5));
        assertEquals(Color.GREEN, gridManager.getStitchColor(20, 5));
    }

    @Test
    public void testDuplicateDown() {
        gridManager.setStitchColor(4, 5, Color.YELLOW);
        gridManager.duplicateDown();
        assertEquals(Color.YELLOW, gridManager.getStitchColor(4, 5));
        assertEquals(Color.YELLOW, gridManager.getStitchColor(20, 5));
    }
}