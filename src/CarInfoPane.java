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
 * The inner panel uses GridBagLayout with a fixed pixel width per column
 * (COLUMN_WIDTHS), so the table's total width is always predictable and
 * fits inside the pane — a long status message can no longer force every
 * column to balloon in width the way a shared GridLayout would. A vertical
 * JScrollPane still handles overflow as more cars are added.
 *
 * Cars are created here via addCar() and stored in the public ArrayList
 * so TrafficAnalysisGUI can iterate them for pause/stop operations.
 */
public class CarInfoPane extends JScrollPane {

    // -----------------------------------------------------------------------
    // Column layout
    // -----------------------------------------------------------------------

    /** Column headers, in display order. */
    private static final String[] HEADERS =
            {"Vehicle #", "Current Activity", "Speed (km/h)", "Position", "Heading"};

    /**
     * Fixed pixel width for each column, in the same order as HEADERS.
     * Pinning these keeps the table's total width constant regardless of
     * cell content, which is what keeps it from running off the pane.
     */
    static final int[] COLUMN_WIDTHS = {70, 150, 100, 90, 75};

    /** Fixed row height applied to every header and data cell. */
    static final int ROW_HEIGHT = 24;

    /** Index of the column that absorbs any extra horizontal space. */
    private static final int STRETCH_COLUMN = 1;

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /** Inner panel that holds all the JTextField rows. */
    private final JPanel innerPanel;

    /** Live list of all cars — read by GridCanvas and the main GUI. */
    public final ArrayList<Car> cars = new ArrayList<>();

    /** Running counter for car creation order (used for naming and color). */
    private int carCount = 1;

    /** Next GridBagLayout row to place a row of cells into (0 = header). */
    private int nextGridRow = 0;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * CarInfoPane - default constructor.
     * Builds the header row and creates the initial set of cars.
     */
    public CarInfoPane() {
        innerPanel = new JPanel(new GridBagLayout());
        innerPanel.setBackground(new Color(45, 45, 45));

        // Wrap in this JScrollPane
        setViewportView(innerPanel);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
        JTextField[] cells = new JTextField[HEADERS.length];
        for (int i = 0; i < HEADERS.length; i++) {
            JTextField hdr = new JTextField(HEADERS[i]);
            hdr.setHorizontalAlignment(SwingConstants.CENTER);
            hdr.setEditable(false);
            hdr.setFont(new Font("SansSerif", Font.BOLD, 11));
            hdr.setBackground(new Color(30, 30, 30));
            hdr.setForeground(Color.WHITE);
            hdr.setBorder(new LineBorder(new Color(80, 80, 80)));
            cells[i] = hdr;
        }
        addRow(cells);
    }

    // -----------------------------------------------------------------------
    // Row placement
    // -----------------------------------------------------------------------

    /**
     * addRow - places one row of cells into the table at the next available
     * grid row, pinning each cell's preferred width to COLUMN_WIDTHS so the
     * table's total width never depends on cell content.
     *
     * @param cells (JTextField[]) exactly COLUMN_WIDTHS.length cells, in column order
     */
    void addRow(JTextField[] cells) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = nextGridRow;
        gbc.fill = GridBagConstraints.BOTH;
        for (int col = 0; col < cells.length; col++) {
            JTextField cell = cells[col];
            cell.setPreferredSize(new Dimension(COLUMN_WIDTHS[col], ROW_HEIGHT));
            gbc.gridx = col;
            gbc.weightx = (col == STRETCH_COLUMN) ? 1.0 : 0.0;
            innerPanel.add(cell, gbc);
        }
        nextGridRow++;
    }

    // -----------------------------------------------------------------------
    // Car management
    // -----------------------------------------------------------------------

    /**
     * addCar - creates a new Car and adds it to the simulation.
     * The Car's constructor appends its own info row to this pane.
     * The panel is then revalidated so Swing redraws the layout.
     */
    public void addCar() {
        Car c = new Car(carCount, this);
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
