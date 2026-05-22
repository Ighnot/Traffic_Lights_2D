import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * TrafficAnalysisGUI.java
 * @author John Leckie (original), refactored for 2D grid
 * CMSC335, Dec 2023, Project 3 — 2D Grid Edition
 *
 * Main class and entry point for the Traffic Flow and Intersection Simulator.
 * Manages a 6x6 grid of intersections (36 total), each with its own
 * TrafficLight and TrafficLightDisplay thread.
 *
 * Layout (BorderLayout on the root content pane):
 *   NORTH  — title bar + system clock
 *   CENTER — GridCanvas (the animated 6x6 road grid)
 *   EAST   — CarInfoPane (scrollable table of car data)
 *   SOUTH  — button bar (Start / Stop / Pause / Continue / Add Car)
 *
 * Static shared resources:
 *   tLights — ArrayList<TrafficLight> accessible by all car threads via
 *             getTrafficSignals(). Synchronized on access.
 *
 * Thread model:
 *   - 1 CurrentTime thread (daemon)
 *   - 36 TrafficLightDisplay threads (one per intersection, daemon)
 *   - N CarMovement threads (one per car, daemon) — N starts at 3, max 6
 *
 * All worker threads are daemon threads so they exit cleanly when the
 * window is closed without needing explicit stop logic at JVM exit.
 */
public class TrafficAnalysisGUI extends JFrame implements ActionListener {

    // -----------------------------------------------------------------------
    // Simulation constants
    // -----------------------------------------------------------------------

    /** Side length of the intersection grid. */
    public static final int GRID_SIZE = GridPosition.GRID_SIZE;

    /** Maximum number of cars the simulation supports. */
    private static final int MAX_CARS = 6;

    // -----------------------------------------------------------------------
    // Static shared data — accessed by CarMovement threads
    // -----------------------------------------------------------------------

    /**
     * Master list of all TrafficLight objects on the grid.
     * Populated in initGrid() and read by CarMovement via getTrafficSignals().
     * Access is synchronized.
     */
    private static final ArrayList<TrafficLight> tLights = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Instance fields — Swing components
    // -----------------------------------------------------------------------

    private JPanel     contentPane;
    private JPanel     topBar;
    private JPanel     buttonPane;
    private GridCanvas gridCanvas;
    private CarInfoPane carInfoPane;

    private JButton btnStart, btnStop, btnPause, btnContinue, btnAddCar;
    private JLabel  lblTitle;

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

    /**
     * main - creates and displays the main application window on the EDT.
     *
     * @param args (String[]) not used
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TrafficAnalysisGUI frame = new TrafficAnalysisGUI();
            frame.setVisible(true);
        });
    }

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * TrafficAnalysisGUI - default constructor.
     * Builds the Swing UI, initializes the 6x6 intersection grid,
     * and starts the clock thread.
     */
    public TrafficAnalysisGUI() {
        initComponents();
        initGrid();
        startClockThread();
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    /**
     * initComponents - constructs all Swing components and assembles the layout.
     * Called once from the constructor; can be called again by startSim() to
     * rebuild after a stop.
     */
    private void initComponents() {
        setTitle("Traffic Flow and Intersection Simulator — 6×6 Grid");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Root content pane — BorderLayout
        contentPane = new JPanel(new BorderLayout(4, 4));
        contentPane.setBackground(new Color(30, 30, 30));
        setContentPane(contentPane);

        // ---- NORTH: title + clock ----
        topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(20, 20, 20));
        topBar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        lblTitle = new JLabel("Traffic Flow & Intersection Simulator — 6×6 Grid");
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        topBar.add(lblTitle, BorderLayout.WEST);

        // Clock label is added by CurrentTime into a wrapper panel
        JPanel clockPanel = new JPanel(new BorderLayout());
        clockPanel.setBackground(new Color(20, 20, 20));
        topBar.add(clockPanel, BorderLayout.EAST);
        contentPane.add(topBar, BorderLayout.NORTH);

        // Start the clock (adds its label to clockPanel)
        Thread clockThread = new Thread(new CurrentTime(clockPanel), "Clock Thread");
        clockThread.setDaemon(true);
        clockThread.start();

        // ---- CENTER: GridCanvas (road grid + cars) ----
        // Built in initGrid() after tLights is populated

        // ---- EAST: CarInfoPane (scrollable car table) ----
        carInfoPane = new CarInfoPane();
        carInfoPane.setPreferredSize(new Dimension(420, 0));
        contentPane.add(carInfoPane, BorderLayout.EAST);

        // ---- SOUTH: button bar ----
        buttonPane = new JPanel(new GridLayout(1, 0, 6, 0));
        buttonPane.setBackground(new Color(20, 20, 20));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        btnStart    = makeButton("Start",    false);
        btnStop     = makeButton("Stop",     true);
        btnPause    = makeButton("Pause",    true);
        btnContinue = makeButton("Continue", false);
        btnAddCar   = makeButton("Add Car",  true);

        buttonPane.add(btnStart);
        buttonPane.add(btnStop);
        buttonPane.add(btnPause);
        buttonPane.add(btnContinue);
        buttonPane.add(btnAddCar);

        contentPane.add(buttonPane, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * makeButton - factory helper that creates a styled JButton and
     * registers this frame as its ActionListener.
     *
     * @param label   (String)  button text
     * @param enabled (boolean) initial enabled state
     * @return configured JButton
     */
    private JButton makeButton(String label, boolean enabled) {
        JButton btn = new JButton(label);
        btn.setEnabled(enabled);
        btn.addActionListener(this);
        btn.setFocusPainted(false);
        return btn;
    }

    // -----------------------------------------------------------------------
    // Grid initialization
    // -----------------------------------------------------------------------

    /**
     * initGrid - creates all 36 TrafficLight objects, starts their display
     * threads, builds the GridCanvas, and adds it to the CENTER of the layout.
     *
     * Called once on startup and again by startSim() after a stop/restart.
     */
    private void initGrid() {
        synchronized (tLights) {
            tLights.clear();

            for (int r = 0; r < GRID_SIZE; r++) {
                for (int c = 0; c < GRID_SIZE; c++) {
                    TrafficLight tl = new TrafficLight(r, c);
                    tLights.add(tl);

                    // Each intersection gets its own display thread
                    Thread t = new Thread(new TrafficLightDisplay(tl),
                                          "Light-" + r + "-" + c);
                    t.setDaemon(true);
                    t.start();
                }
            }
        }

        // Build the canvas with a reference to the live car list
        gridCanvas = new GridCanvas(carInfoPane.cars);
        contentPane.add(gridCanvas, BorderLayout.CENTER);
        contentPane.revalidate();
        pack();
        setLocationRelativeTo(null);
    }

    // -----------------------------------------------------------------------
    // Clock thread
    // -----------------------------------------------------------------------

    /**
     * startClockThread - starts the CurrentTime daemon thread.
     * The clock label is embedded in the topBar's EAST clock panel,
     * which is added by the CurrentTime constructor.
     *
     * Note: clock is started in initComponents() directly for simplicity.
     * This method is kept as a named entry point for clarity.
     */
    private void startClockThread() {
        // Already started inside initComponents() — no-op here.
        // Retained as a named method to document intent.
    }

    // -----------------------------------------------------------------------
    // Simulation control
    // -----------------------------------------------------------------------

    /**
     * startSim - resets and restarts the simulation.
     * Clears existing lights and cars, rebuilds the grid and car info pane,
     * and re-enables the appropriate buttons.
     */
    private void startSim() {
        // Remove old canvas
        contentPane.remove(gridCanvas);

        // Rebuild car info pane (removes old cars)
        contentPane.remove(carInfoPane);
        carInfoPane = new CarInfoPane();
        carInfoPane.setPreferredSize(new Dimension(420, 0));
        contentPane.add(carInfoPane, BorderLayout.EAST);

        // Rebuild the light grid and canvas
        initGrid();

        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        btnPause.setEnabled(true);
        btnContinue.setEnabled(false);
        btnAddCar.setEnabled(true);
    }

    /**
     * stopSim - halts all car and light threads, removes the canvas,
     * and updates button states.
     */
    private void stopSim() {
        // Signal all cars and lights to stop
        synchronized (carInfoPane.cars) {
            for (Car car : carInfoPane.cars) {
                car.setStopped(true);
            }
        }
        synchronized (tLights) {
            for (TrafficLight tl : tLights) {
                tl.setStopped(true);
            }
        }

        carInfoPane.stopSim();

        // Remove the canvas
        contentPane.remove(gridCanvas);
        contentPane.revalidate();
        contentPane.repaint();

        btnStop.setEnabled(false);
        btnStart.setEnabled(true);
        btnPause.setEnabled(false);
        btnContinue.setEnabled(false);
        btnAddCar.setEnabled(false);
    }

    /**
     * pauseSim - sets the isPaused flag on all lights and cars,
     * freezing all motion without stopping threads.
     */
    private void pauseSim() {
        synchronized (tLights) {
            for (TrafficLight tl : tLights) { tl.setPaused(true); }
        }
        synchronized (carInfoPane.cars) {
            for (Car car : carInfoPane.cars) { car.setPaused(true); }
        }
        btnPause.setEnabled(false);
        btnContinue.setEnabled(true);
        btnStop.setEnabled(false);
        btnAddCar.setEnabled(true);
    }

    /**
     * resumeSim - clears the isPaused flag on all lights and cars,
     * allowing threads to resume their normal loops.
     */
    private void resumeSim() {
        synchronized (tLights) {
            for (TrafficLight tl : tLights) { tl.setPaused(false); }
        }
        synchronized (carInfoPane.cars) {
            for (Car car : carInfoPane.cars) { car.setPaused(false); }
        }
        btnPause.setEnabled(true);
        btnContinue.setEnabled(false);
        btnStop.setEnabled(true);
        btnAddCar.setEnabled(carInfoPane.cars.size() < MAX_CARS);
    }

    /**
     * addCar - adds one more car to the simulation, up to MAX_CARS.
     */
    private void addCar() {
        carInfoPane.addCar();
        if (carInfoPane.cars.size() >= MAX_CARS) {
            btnAddCar.setEnabled(false);
        }
    }

    // -----------------------------------------------------------------------
    // Static accessors — called by CarMovement threads
    // -----------------------------------------------------------------------

    /**
     * getTrafficSignals - returns a snapshot of the current traffic light list.
     * Synchronized to prevent ConcurrentModificationException when the list
     * is rebuilt on restart.
     *
     * @return ArrayList<TrafficLight> current list of all intersection lights
     */
    public static synchronized ArrayList<TrafficLight> getTrafficSignals() {
        return new ArrayList<>(tLights);
    }

    // -----------------------------------------------------------------------
    // ActionListener
    // -----------------------------------------------------------------------

    /**
     * actionPerformed - routes button clicks to the appropriate sim method.
     *
     * @param e (ActionEvent) the event generated by a button press
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if      (src == btnStart)    startSim();
        else if (src == btnStop)     stopSim();
        else if (src == btnPause)    pauseSim();
        else if (src == btnContinue) resumeSim();
        else if (src == btnAddCar)   addCar();
    }
}
