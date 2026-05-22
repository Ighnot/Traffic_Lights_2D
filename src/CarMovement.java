import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CarMovement.java
 * @author John Leckie (original CarSpeedAndPosition), refactored for 2D grid
 * CMSC335, Dec 2023, Project 3 — 2D Grid Edition
 *
 * The primary Runnable class governing each car's behavior on the 6x6 grid.
 *
 * Responsibilities:
 *   1. Move the car smoothly between intersections using sub-cell pixel offsets.
 *   2. React to the traffic light at the current intersection:
 *        GREEN  → drive at normal speed
 *        YELLOW → slow down
 *        RED    → stop until the light changes
 *   3. At each intersection, randomly choose to go STRAIGHT, LEFT, or RIGHT.
 *      If the chosen direction would leave the grid, alternate choices are tried.
 *      As a last resort, the car reverses (U-turns) to stay on the grid.
 *   4. Push updated state back to the Car object so the GridCanvas and
 *      CarInfoPane reflect real-time position and status.
 *
 * Movement model:
 *   The car moves STEPS_PER_SEGMENT sub-steps between intersections.
 *   Each sub-step advances the pixel offset by (CELL_SIZE / STEPS_PER_SEGMENT).
 *   Speed (km/h) controls the sleep delay between sub-steps: faster cars
 *   have shorter sleep times and therefore appear to move more quickly.
 *
 * Thread safety:
 *   All fields on Car that are read by the GridCanvas (gridRow, gridCol,
 *   pixelOffsetX, pixelOffsetY, heading) are declared volatile on Car.
 *   TrafficAnalysisGUI.getTrafficSignals() is synchronized.
 */
public class CarMovement implements Runnable {

    // -----------------------------------------------------------------------
    // Movement constants
    // -----------------------------------------------------------------------

    /** Number of pixel sub-steps taken between two adjacent intersections. */
    private static final int STEPS_PER_SEGMENT = 20;

    /** Pixel distance moved per sub-step (== CELL_SIZE / STEPS_PER_SEGMENT). */
    private static final int PIXELS_PER_STEP = GridPosition.CELL_SIZE / STEPS_PER_SEGMENT;

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /** The car this Runnable is driving. */
    private final Car car;

    /** Current grid position of the car. */
    private int row, col;

    /** Current heading. */
    private Direction heading;

    /** Loop-control flag — mirrors Car.isStopped. */
    private volatile boolean isStopped;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * CarMovement - parameterized constructor.
     * Picks a random starting position and heading for the car,
     * then pushes that initial state to the Car model.
     *
     * @param car (Car) the Car object this Runnable will drive
     */
    public CarMovement(Car car) {
        this.car      = car;
        this.isStopped = false;

        // Random starting position anywhere on the 6x6 grid
        row = ThreadLocalRandom.current().nextInt(0, GridPosition.GRID_SIZE);
        col = ThreadLocalRandom.current().nextInt(0, GridPosition.GRID_SIZE);

        // Random starting heading
        Direction[] dirs = Direction.values();
        heading = dirs[ThreadLocalRandom.current().nextInt(dirs.length)];

        // Push initial state to the car model
        pushStateToCar("Starting");
    }

    // -----------------------------------------------------------------------
    // Runnable implementation
    // -----------------------------------------------------------------------

    /**
     * run - main loop.
     * On each iteration the car checks for pause/stop, reacts to the
     * traffic light at its current intersection, then (if green) moves
     * one full segment to the next intersection.
     */
    @Override
    public void run() {
        while (!isStopped) {

            // --- Check pause ---
            if (car.isPaused()) {
                pushStateToCar("Paused");
                sleepMs(200);
                continue;
            }

            // --- React to current intersection's light ---
            TrafficLight light = getLightAt(row, col);
            if (light == null) {
                // No light data yet — wait briefly and retry
                sleepMs(300);
                continue;
            }

            String color = light.getColor();

            if ("red".equalsIgnoreCase(color)) {
                // ---- RED: stop at intersection ----
                car.speed  = 0;
                car.pixelOffsetX = 0;
                car.pixelOffsetY = 0;
                pushStateToCar("Stopped at " + light.getIntersectionName());
                sleepMs(400);   // check the light again shortly

            } else if ("yellow".equalsIgnoreCase(color)) {
                // ---- YELLOW: slow down but keep moving ----
                int slowSpeed = ThreadLocalRandom.current().nextInt(15, 30);
                car.speed = slowSpeed;
                pushStateToCar("Slowing for " + light.getIntersectionName());
                // Travel one segment at reduced speed (longer sleep per step)
                travelSegment(slowSpeed);

            } else {
                // ---- GREEN: full speed ----
                int fullSpeed = ThreadLocalRandom.current().nextInt(40, 70);
                car.speed = fullSpeed;
                pushStateToCar("Driving");
                travelSegment(fullSpeed);
            }

            // --- Check stop flag ---
            isStopped = car.isStopped();
            if (isStopped) {
                car.speed  = 0;
                car.status = "STOPPED";
                car.updateInfo();
            }
        }
    }

    // -----------------------------------------------------------------------
    // Movement helpers
    // -----------------------------------------------------------------------

    /**
     * travelSegment - animates the car moving from its current intersection
     * to the next one in the current (or newly chosen) heading.
     *
     * Before moving, the car chooses whether to go straight, turn left,
     * or turn right. If the preferred direction leads off the grid, alternate
     * directions are tried until a valid one is found.
     *
     * The movement is broken into STEPS_PER_SEGMENT pixel sub-steps so the
     * GridCanvas shows smooth motion rather than teleportation.
     *
     * @param speed (int) km/h — used to compute sleep time per sub-step
     */
    private void travelSegment(int speed) {
        // Choose next direction at this intersection
        heading = chooseDirection();
        car.heading = heading;

        // Determine pixel delta per step based on heading
        int dxPerStep = 0, dyPerStep = 0;
        switch (heading) {
            case EAST:  dxPerStep =  PIXELS_PER_STEP; break;
            case WEST:  dxPerStep = -PIXELS_PER_STEP; break;
            case SOUTH: dyPerStep =  PIXELS_PER_STEP; break;
            case NORTH: dyPerStep = -PIXELS_PER_STEP; break;
        }

        // Sleep time per sub-step: faster speed → shorter sleep
        // Base: 50 km/h → 60 ms/step.  Scales inversely with speed.
        long msPerStep = Math.max(20L, (long)(3000.0 / speed));

        // Animate across the segment
        for (int step = 0; step < STEPS_PER_SEGMENT; step++) {
            if (car.isStopped() || car.isPaused()) break;

            car.pixelOffsetX += dxPerStep;
            car.pixelOffsetY += dyPerStep;
            sleepMs(msPerStep);
        }

        // Snap to next intersection center and reset pixel offset
        GridPosition next = new GridPosition(row, col).neighbor(heading);
        if (next.isValid()) {
            row = next.row;
            col = next.col;
        }
        car.gridRow = row;
        car.gridCol = col;
        car.pixelOffsetX = 0;
        car.pixelOffsetY = 0;
    }

    /**
     * chooseDirection - randomly selects STRAIGHT, LEFT, or RIGHT at the
     * current intersection. If the preferred choice would move the car off
     * the grid, the other two options are tried in order. If all three
     * lead off the grid (corner with no valid exits in three directions),
     * the car reverses (U-turn) as a fallback.
     *
     * @return Direction the car will travel next
     */
    private Direction chooseDirection() {
        // Build a shuffled list of candidates: [straight, left, right]
        int roll = ThreadLocalRandom.current().nextInt(3);
        Direction[] candidates = new Direction[3];
        candidates[0] = (roll == 0) ? heading          :
                        (roll == 1) ? heading.turnLeft():
                                      heading.turnRight();
        candidates[1] = (roll == 0) ? heading.turnLeft()  :
                        (roll == 1) ? heading             :
                                      heading.turnLeft();
        candidates[2] = (roll == 0) ? heading.turnRight() :
                        (roll == 1) ? heading.turnRight()  :
                                      heading;

        GridPosition here = new GridPosition(row, col);

        // Return the first candidate that stays on the grid
        for (Direction d : candidates) {
            if (here.neighbor(d).isValid()) {
                return d;
            }
        }

        // Fallback: reverse direction (U-turn) — always valid unless 1x1 grid
        return heading.turnLeft().turnLeft();
    }

    // -----------------------------------------------------------------------
    // Traffic light lookup
    // -----------------------------------------------------------------------

    /**
     * getLightAt - finds the TrafficLight at the given (row, col) position
     * from the master list maintained by TrafficAnalysisGUI.
     *
     * @param r (int) grid row
     * @param c (int) grid column
     * @return TrafficLight at that position, or null if not found
     */
    private TrafficLight getLightAt(int r, int c) {
        ArrayList<TrafficLight> lights = TrafficAnalysisGUI.getTrafficSignals();
        for (TrafficLight tl : lights) {
            GridPosition gp = tl.getGridPos();
            if (gp.row == r && gp.col == c) {
                return tl;
            }
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // State synchronization
    // -----------------------------------------------------------------------

    /**
     * pushStateToCar - writes the current row, col, and heading into the
     * Car's volatile fields and updates the status string, then triggers
     * an info-pane refresh.
     *
     * @param statusMsg (String) human-readable status to display
     */
    private void pushStateToCar(String statusMsg) {
        car.gridRow = row;
        car.gridCol = col;
        car.heading = heading;
        car.status  = statusMsg;
        car.updateInfo();
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    /**
     * sleepMs - sleeps the current thread for the given number of milliseconds.
     * Swallows InterruptedException and re-interrupts the thread.
     *
     * @param ms (long) milliseconds to sleep
     */
    private void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
