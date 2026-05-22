import java.awt.*;
import javax.swing.*;
import javax.swing.border.LineBorder;

/**
 * Car.java
 * @author John Leckie (original), refactored for 2D grid
 * CMSC335, Dec 2023, Project 3 — 2D Grid Edition
 *
 * Represents one car in the simulation. A Car tracks its own:
 *   - Grid position (row, col) on the 6x6 intersection grid
 *   - Sub-cell pixel offset so it appears to move smoothly between intersections
 *   - Current heading (Direction: NORTH, EAST, SOUTH, WEST)
 *   - Speed, status string, and pause/stop flags
 *
 * Each Car also owns a row of JTextFields in the CarInfoPane that displays
 * its current state. The CarMovement Runnable updates these via updateInfo().
 *
 * Each Car runs in its own thread via CarMovement.
 *
 * Color assignment: cars are given distinct colors from a fixed palette
 * so they can be distinguished on the GridCanvas.
 */
public class Car {

    // -----------------------------------------------------------------------
    // Palette of distinct car colors — one per car in creation order
    // -----------------------------------------------------------------------
    private static final Color[] CAR_COLORS = {
        new Color(220,  50,  50),  // red
        new Color(  0, 130, 200),  // blue
        new Color( 60, 180,  75),  // green
        new Color(255, 165,   0),  // orange
        new Color(145,  30, 180),  // purple
        new Color(  0, 200, 200),  // cyan
    };

    // -----------------------------------------------------------------------
    // Identity and display
    // -----------------------------------------------------------------------

    /** Human-readable name, e.g. "Car 1". */
    public final String carName;

    /** Color used to render this car on the GridCanvas. */
    public final Color carColor;

    // -----------------------------------------------------------------------
    // State — read by GridCanvas and CarInfoPane from the EDT and car threads
    // Use volatile for fields accessed from multiple threads without sync blocks.
    // -----------------------------------------------------------------------

    /** Current grid row (intersection row the car is at or moving toward). */
    public volatile int gridRow;

    /** Current grid column (intersection column the car is at or moving toward). */
    public volatile int gridCol;

    /**
     * Sub-cell pixel offset from the intersection center.
     * Ranges from 0 (at intersection) toward GridPosition.CELL_SIZE (next intersection).
     * Used by GridCanvas to interpolate the car's painted position between nodes.
     */
    public volatile int pixelOffsetX;
    public volatile int pixelOffsetY;

    /** Current cardinal direction the car is traveling. */
    public volatile Direction heading;

    /** Current speed in km/h (used for display; controls sleep duration in CarMovement). */
    public volatile int speed;

    /** Human-readable status string, e.g. "Driving", "Stopped at (2,3)". */
    public volatile String status;

    // -----------------------------------------------------------------------
    // Simulation control flags
    // -----------------------------------------------------------------------

    /** Set to true by the GUI when the user clicks Pause. */
    public volatile boolean isPaused;

    /** Set to true by the GUI when the user clicks Stop. */
    public volatile boolean isStopped;

    // -----------------------------------------------------------------------
    // Info pane Swing components — updated on car thread, read on EDT.
    // In a production app these would be updated via SwingUtilities.invokeLater;
    // direct update is retained here for consistency with the original design.
    // -----------------------------------------------------------------------

    private final JPanel infoPane;
    private JTextField txtName, txtStatus, txtSpeed, txtPosition, txtDirection;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Car - parameterized constructor.
     * Assigns identity, picks a color, initializes state, builds info-pane
     * components, and starts the CarMovement thread.
     *
     * @param carCount  (int) creation-order index, used for naming and color
     * @param carInfoPane (JPanel) shared panel that holds all car info rows
     */
    public Car(int carCount, JPanel carInfoPane) {
        this.carName  = "Car " + carCount;
        this.carColor = CAR_COLORS[(carCount - 1) % CAR_COLORS.length];
        this.infoPane = carInfoPane;

        // Default state — CarMovement will randomize on first tick
        this.gridRow      = 0;
        this.gridCol      = 0;
        this.pixelOffsetX = 0;
        this.pixelOffsetY = 0;
        this.heading      = Direction.EAST;
        this.speed        = 0;
        this.status       = "Starting";
        this.isPaused     = false;
        this.isStopped    = false;

        // Build Swing info row before starting the thread
        addInfoRow();

        // Start the car's movement thread
        Thread t = new Thread(new CarMovement(this), carName);
        t.setDaemon(true);   // thread exits automatically when the JVM shuts down
        t.start();
    }

    // -----------------------------------------------------------------------
    // Info pane construction
    // -----------------------------------------------------------------------

    /**
     * addInfoRow - creates and appends a row of JTextFields to the shared
     * carInfoPane. Each field corresponds to one column in the car table:
     * Name | Status | Speed | Position | Direction.
     */
    private void addInfoRow() {
        LineBorder border = new LineBorder(Color.BLACK);

        // Name cell — colored background matches the car's canvas color
        txtName = new JTextField(carName);
        txtName.setHorizontalAlignment(SwingConstants.CENTER);
        txtName.setBackground(carColor.brighter());
        txtName.setEditable(false);
        txtName.setBorder(border);
        infoPane.add(txtName);

        // Status
        txtStatus = new JTextField(status);
        txtStatus.setHorizontalAlignment(SwingConstants.CENTER);
        txtStatus.setEditable(false);
        txtStatus.setBorder(border);
        infoPane.add(txtStatus);

        // Speed
        txtSpeed = new JTextField("0 km/h");
        txtSpeed.setHorizontalAlignment(SwingConstants.CENTER);
        txtSpeed.setEditable(false);
        txtSpeed.setBorder(border);
        infoPane.add(txtSpeed);

        // Grid position
        txtPosition = new JTextField("(0,0)");
        txtPosition.setHorizontalAlignment(SwingConstants.CENTER);
        txtPosition.setEditable(false);
        txtPosition.setBorder(border);
        infoPane.add(txtPosition);

        // Direction / heading
        txtDirection = new JTextField(heading.toArrow());
        txtDirection.setHorizontalAlignment(SwingConstants.CENTER);
        txtDirection.setEditable(false);
        txtDirection.setBorder(border);
        infoPane.add(txtDirection);
    }

    // -----------------------------------------------------------------------
    // State update — called by CarMovement after each movement tick
    // -----------------------------------------------------------------------

    /**
     * updateInfo - pushes the current state fields into the Swing text fields
     * so the info table reflects the car's latest position, speed, and status.
     *
     * Called from the car's own thread. Swing thread safety is handled the
     * same way as the original project (direct field update).
     */
    public void updateInfo() {
        txtStatus.setText(status);
        txtSpeed.setText(speed + " km/h");
        txtPosition.setText("(" + gridRow + "," + gridCol + ")");
        txtDirection.setText(heading.toArrow() + " " + heading.name());
    }

    // -----------------------------------------------------------------------
    // Accessors used by CarMovement
    // -----------------------------------------------------------------------

    public boolean isPaused()  { return isPaused;  }
    public boolean isStopped() { return isStopped; }

    public void setPaused(boolean p)  { this.isPaused  = p; }
    public void setStopped(boolean s) { this.isStopped = s; }
}
