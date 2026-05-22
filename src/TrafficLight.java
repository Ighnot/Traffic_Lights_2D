import javax.swing.*;

/**
 * TrafficLight.java
 * @author John Leckie (original), refactored for 2D grid
 * CMSC335, Dec 2023, Project 3 — 2D Grid Edition
 *
 * Represents one traffic light at a specific grid intersection.
 * Each TrafficLight knows its (row, col) position on the 6x6 grid,
 * its current color, and whether the simulation is paused or stopped.
 *
 * The TrafficLight extends JLabel so it can display its light icon
 * directly in a Swing layout if needed, but the primary visual
 * representation is handled by GridCanvas using the color string.
 *
 * Thread safety: color, name, and position are accessed by both
 * car threads and the light display thread, so accessors are synchronized.
 */
public class TrafficLight extends JLabel {

    /** Human-readable name, e.g. "Intersection (2,3)". */
    private final String name;

    /** Grid position of this intersection. */
    private final GridPosition gridPos;

    /** Current light color: "red", "yellow", or "green". */
    private String color;

    /** Pause and stop flags, set by the main GUI thread. */
    volatile boolean isPaused;
    volatile boolean isStopped;

    /**
     * TrafficLight - parameterized constructor.
     * Initializes the light at a specific grid position.
     *
     * @param row (int) 0-based grid row
     * @param col (int) 0-based grid column
     */
    public TrafficLight(int row, int col) {
        this.gridPos = new GridPosition(row, col);
        this.name = "Intersection " + gridPos.toString();
        this.color = "red";   // safe default before TrafficLightDisplay sets it
        this.isPaused = false;
        this.isStopped = false;
    }

    // -----------------------------------------------------------------------
    // Synchronized accessors — called from multiple threads
    // -----------------------------------------------------------------------

    /**
     * getColor - returns the current color of this light.
     *
     * @return color string: "red", "yellow", or "green"
     */
    public synchronized String getColor() {
        return color;
    }

    /**
     * setColor - sets the current color of this light.
     * Called by TrafficLightDisplay on each color transition.
     *
     * @param color (String) "red", "yellow", or "green"
     */
    public synchronized void setColor(String color) {
        this.color = color;
    }

    /**
     * getIntersectionName - returns the human-readable name.
     *
     * @return name (String)
     */
    public synchronized String getIntersectionName() {
        return name;
    }

    /**
     * getGridPos - returns the grid position of this intersection.
     *
     * @return GridPosition (row, col)
     */
    public synchronized GridPosition getGridPos() {
        return gridPos;
    }

    // -----------------------------------------------------------------------
    // Pause / stop flags — volatile, no sync needed for boolean reads
    // -----------------------------------------------------------------------

    /** @return true if the simulation is paused */
    public boolean isPaused() {
        return isPaused;
    }

    /** @param isPaused true to pause, false to resume */
    public void setPaused(boolean isPaused) {
        this.isPaused = isPaused;
    }

    /** @return true if the simulation has been stopped */
    public boolean isStopped() {
        return isStopped;
    }

    /** @param isStopped true to stop the light thread */
    public void setStopped(boolean isStopped) {
        this.isStopped = isStopped;
    }
}
