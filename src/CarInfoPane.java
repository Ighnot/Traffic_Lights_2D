import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.LineBorder;

/**
 * CarInfoPane.java
 * @author John Leckie (original), refactored for 2D grid
 * CMSC335, Dec 2023, Project 3 — 2D Grid Edition
 *
 * A scrollable JPanel that displays a table of real-time car information.
 * Each row corresponds to one Car in the simulation and shows:
 *   Vehicle # | Current Activity | Speed | Grid Position | Heading
 *
 * The panel uses a GridLayout with 5 columns. A JScrollPane wraps it
 * so additional cars don't overflow the UI when cars are added.
 *
 * Cars are created here via addCar() and stored in the public ArrayList
 * so TrafficAnalysisGUI can iterate them for pause/stop operations.
 */
public class CarInfoPane extends JScrollPane {

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /** Inner panel that holds all the JTextField rows. */
    private final JPanel innerPanel;

    /** Live list of all cars — read by GridCanvas and the main GUI. */
    public final ArrayList<Car> cars = new ArrayList<>();

    /** Running counter for car creation order (used for naming and color). */
    private int carCount = 1;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * CarInfoPane - default constructor.
     * Builds the header row and creates the initial set of cars.
     */
    public CarInfoPane() {
        // Inner panel uses a 5-column GridLayout; rows grow as cars are added
        innerPanel = new JPanel(new GridLayout(0, 5, 0, 0));
        innerPanel.setBackground(new Color(45, 45, 45));

        // Wrap in this JScrollPane
        setViewportView(innerPanel);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        getVerticalScrollBar().setUnitIncrement(16);

        buildHeader();

        // Create the default starting cars
        for (int i = 0; i < 3; i++) {
            addCar();
        }
    }

    // -----------------------------------------------------------------------
    // Header row
    // -----------------------------------------------------------------------

    /**
     * buildHeader - creates the column label row at the top of the table.
     * Uses bold, non-editable JTextFields styled with a dark background.
     */
    private void buildHeader() {
        String[] headers = {"Vehicle #", "Current Activity", "Speed (km/h)",
                            "Position (row,col)", "Heading"};
        for (String h : headers) {
            JTextField hdr = new JTextField(h);
            hdr.setHorizontalAlignment(SwingConstants.CENTER);
            hdr.setEditable(false);
            hdr.setFont(new Font("SansSerif", Font.BOLD, 11));
            hdr.setBackground(new Color(30, 30, 30));
            hdr.setForeground(Color.WHITE);
            hdr.setBorder(new LineBorder(new Color(80, 80, 80)));
            innerPanel.add(hdr);
        }
    }

    // -----------------------------------------------------------------------
    // Car management
    // -----------------------------------------------------------------------

    /**
     * addCar - creates a new Car and adds it to the simulation.
     * The Car's constructor appends its own info row to innerPanel.
     * The panel is then revalidated so Swing redraws the layout.
     */
    public void addCar() {
        Car c = new Car(carCount, innerPanel);
        cars.add(c);
        carCount++;
        innerPanel.revalidate();
        innerPanel.repaint();
    }

    /**
     * stopSim - clears all car data from the inner panel and replaces it
     * with a centered "SIMULATION STOPPED" label.
     */
    public void stopSim() {
        innerPanel.removeAll();
        innerPanel.setLayout(new BorderLayout());
        JLabel stopped = new JLabel("SIMULATION STOPPED");
        stopped.setHorizontalAlignment(SwingConstants.CENTER);
        stopped.setForeground(Color.WHITE);
        stopped.setFont(new Font("SansSerif", Font.BOLD, 16));
        innerPanel.add(stopped, BorderLayout.CENTER);
        innerPanel.revalidate();
        innerPanel.repaint();
    }
}
