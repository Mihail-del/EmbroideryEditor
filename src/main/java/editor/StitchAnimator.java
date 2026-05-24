package editor;

import javafx.animation.AnimationTimer;

/**
 * Drives a radial "bloom" animation for the stitch canvas.
 * Each filled stitch cell fades from fully transparent to fully opaque,
 * starting at the center of the grid and rippling outward. The delay
 * for each cell is proportional to its Euclidean distance from the
 * grid center.
 */
public class StitchAnimator {

    /** Total wall-clock duration of the entire animation (nanoseconds). */
    private static final double TOTAL_DURATION_NS = 10.5e9;

    /** How much of the total duration is used to stagger start times. */
    private static final double DELAY_SPREAD_NS = 0.8e9;

    /** Duration of an individual cell's fade-in (nanoseconds). */
    private static final double CELL_FADE_NS = 3.7e9;

    private double[][] opacities;
    private double[][] startDelays;
    private boolean animating;
    private AnimationTimer timer;
    private long startTimeNs;

    private Runnable onComplete;

    /**
     * @return true while the animation is actively running.
     */
    public boolean isAnimating() {
        return animating;
    }

    /**
     * Initializes the animation state without starting a real-time timer.
     * Useful for manual frame generation.
     * 
     * @param gridManager provides grid size and stitch data
     */
    public void setup(GridManager gridManager) {
        stop();

        int size = gridManager.getGridSize();
        opacities = new double[size][size];
        startDelays = new double[size][size];

        double centerRow = (size - 1) / 2.0;
        double centerCol = (size - 1) / 2.0;

        double maxDist = 0.0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (gridManager.getStitchColor(r, c) != null) {
                    double dr = r - centerRow;
                    double dc = c - centerCol;
                    double dist = Math.sqrt(dr * dr + dc * dc);
                    maxDist = Math.max(maxDist, dist);
                }
            }
        }

        double effectiveMaxDist = Math.max(maxDist, 1.0);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                double dr = r - centerRow;
                double dc = c - centerCol;
                double dist = Math.sqrt(dr * dr + dc * dc);
                startDelays[r][c] = (dist / effectiveMaxDist) * DELAY_SPREAD_NS;
                opacities[r][c] = 0.0;
            }
        }

        animating = true;
    }

    /**
     * Manually step the animation to a specific elapsed time.
     * 
     * @param elapsedNs elapsed time in nanoseconds
     * @return true if the animation is completely finished, false otherwise
     */
    public boolean setElapsed(double elapsedNs) {
        if (!animating || opacities == null)
            return true;

        boolean allDone = true;
        for (int r = 0; r < opacities.length; r++) {
            for (int c = 0; c < opacities[0].length; c++) {
                double cellElapsed = elapsedNs - startDelays[r][c];
                if (cellElapsed <= 0) {
                    opacities[r][c] = 0.0;
                    allDone = false;
                } else if (cellElapsed >= CELL_FADE_NS) {
                    opacities[r][c] = 1.0;
                } else {
                    double t = cellElapsed / CELL_FADE_NS;
                    opacities[r][c] = 1.0 - (1.0 - t) * (1.0 - t);
                    allDone = false;
                }
            }
        }
        return allDone || elapsedNs >= TOTAL_DURATION_NS;
    }

    /**
     * Gets the total duration of the animation in nanoseconds.
     */
    public double getTotalDurationNs() {
        return TOTAL_DURATION_NS;
    }

    /**
     * Returns the current opacity for a specific cell.
     *
     * @param row grid row
     * @param col grid column
     * @return opacity in [0.0, 1.0]; defaults to 1.0 when not animating
     */
    public double getOpacity(int row, int col) {
        if (!animating || opacities == null) {
            return 1.0;
        }
        if (row < 0 || row >= opacities.length || col < 0 || col >= opacities[0].length) {
            return 1.0;
        }
        return opacities[row][col];
    }

    /**
     * Starts the radial bloom animation.
     *
     * @param gridManager provides grid size and stitch data
     * @param onFrame     callback invoked every frame so the render engine can
     *                    repaint
     * @param onComplete  callback invoked when the animation finishes naturally
     */
    public void play(GridManager gridManager, Runnable onFrame, Runnable onComplete) {
        this.onComplete = onComplete;
        setup(gridManager);

        startTimeNs = -1;

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (startTimeNs < 0) {
                    startTimeNs = now;
                }
                double elapsed = now - startTimeNs;

                boolean finished = setElapsed(elapsed);

                onFrame.run();

                if (finished) {
                    stopInternal();
                }
            }
        };
        timer.start();
    }

    /**
     * Stops the animation immediately and resets all opacities to fully visible.
     */
    public void stop() {
        stopInternal();
    }

    private void stopInternal() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        animating = false;
        opacities = null;
        startDelays = null;

        if (onComplete != null) {
            Runnable cb = onComplete;
            onComplete = null;
            cb.run();
        }
    }
}
