import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

/**
 * GridCanvas.java
 * @author John Leckie (original), new class for 2D grid
 * CMSC335, Dec 2023, Project 3 — 2D Grid Edition
 *
 * A custom JPanel that renders the entire 6x6 road grid graphically.
 *
 * What is painted each frame (via a Swing Timer calling repaint()):
 *   1. Gray background
 *   2. Road segments between every adjacent intersection (dark gray stripes)
 *   3. Dashed center-line dividing each road into two lanes
 *   4. Intersection nodes — colored circles whose fill reflects the
 *      traffic light color (red / yellow / green)
 *   5. Intersection labels (row, col)
 *   6. Cars — colored rectangles with an arrow indicating heading,
 *      positioned using sub-cell pixel offsets for smooth animation
 *
 * The canvas polls TrafficAnalysisGUI's static lists each repaint cycle
 * so it always reflects the current simulation state.
 *
 * Size: GRID_SIZE * CELL_SIZE + 2 * GRID_OFFSET in each dimension.
 * With GRID_SIZE=6, CELL_SIZE=100, GRID_OFFSET=50 → 700 × 700 pixels.
 */
public class GridCanvas extends JPanel {

    // -----------------------------------------------------------------------
    // Rendering constants
    // -----------------------------------------------------------------------

    /** Diameter of the circle drawn at each intersection node. */
    private static final int NODE_DIAMETER = 28;

    /** Half-width of the road stripe on each side of center. */
    private static final int ROAD_HALF_WIDTH = 14;

    /** Width (pixels) of the painted car rectangle. */
    private static final int CAR_WIDTH = 18;

    /** Height (pixels) of the painted car rectangle. */
    private static final int CAR_HEIGHT = 10;

    // -----------------------------------------------------------------------
    // Color palette
    // -----------------------------------------------------------------------

    private static final Color COLOR_BACKGROUND  = new Color(60, 60, 60);
    private static final Color COLOR_ROAD        = new Color(40, 40, 40);
    private static final Color COLOR_ROAD_EDGE   = new Color(90, 90, 90);
    private static final Color COLOR_CENTER_LINE = new Color(220, 220, 50);
    private static final Color COLOR_NODE_BORDER = Color.WHITE;
    private static final Color COLOR_LABEL       = new Color(200, 200, 200);

    // Traffic light node fill colors
    private static final Color COLOR_RED    = new Color(210,  40,  40);
    private static final Color COLOR_YELLOW = new Color(220, 190,  30);
    private static final Color COLOR_GREEN  = new Color( 30, 180,  60);
    private static final Color COLOR_UNKNOWN= new Color(100, 100, 100);

    // -----------------------------------------------------------------------
    // Reference to live simulation data — supplied by constructor
    // -----------------------------------------------------------------------

    /** Reference to the car info list, refreshed each paint cycle. */
    private final List<Car> cars;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * GridCanvas - constructor.
     * Sets preferred size and background, then starts a Swing Timer
     * to repaint at ~30 fps (every 33 ms) for smooth car animation.
     *
     * @param cars (List<Car>) live list of all cars in the simulation
     */
    public GridCanvas(List<Car> cars) {
        this.cars = cars;

        int canvasSize = 2 * GridPosition.GRID_OFFSET + GridPosition.GRID_SIZE * GridPosition.CELL_SIZE;
        setPreferredSize(new Dimension(canvasSize, canvasSize));
        setBackground(COLOR_BACKGROUND);

        // Repaint timer — fires every 33 ms (~30 fps)
        Timer repaintTimer = new Timer(20, e -> repaint());
        repaintTimer.start();
    }

    // -----------------------------------------------------------------------
    // Painting
    // -----------------------------------------------------------------------

    /**
     * paintComponent - overrides JPanel's paint method.
     * Called by Swing on the EDT each time repaint() fires.
     * Draws the full scene in z-order: background → roads → nodes → cars.
     *
     * @param g (Graphics) provided by Swing
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Enable anti-aliasing for smoother edges on circles and arrows
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawRoads(g2);
        drawIntersections(g2);
        drawCars(g2);
    }

    // -----------------------------------------------------------------------
    // Road drawing
    // -----------------------------------------------------------------------

    /**
     * drawRoads - paints horizontal and vertical road segments connecting
     * every adjacent pair of intersections on the grid.
     * Each road is a filled rectangle (dark) with edge lines and a dashed
     * center line to simulate two lanes.
     *
     * @param g2 (Graphics2D)
     */
    private void drawRoads(Graphics2D g2) {
        int size = GridPosition.GRID_SIZE;

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int x = GridPosition.GRID_OFFSET + c * GridPosition.CELL_SIZE;
                int y = GridPosition.GRID_OFFSET + r * GridPosition.CELL_SIZE;

                // Horizontal segment → east neighbor
                if (c + 1 < size) {
                    int nx = x + GridPosition.CELL_SIZE;
                    drawRoadSegment(g2, x, y, nx, y, true);
                }
                // Vertical segment → south neighbor
                if (r + 1 < size) {
                    int ny = y + GridPosition.CELL_SIZE;
                    drawRoadSegment(g2, x, y, x, ny, false);
                }
            }
        }
    }

    /**
     * drawRoadSegment - paints one road segment between two intersection centers.
     * Draws a filled road body, edge lines, and a dashed center divider.
     *
     * @param g2         (Graphics2D)
     * @param x1, y1     pixel coordinates of the first intersection center
     * @param x2, y2     pixel coordinates of the second intersection center
     * @param horizontal true if this segment runs east-west
     */
    private void drawRoadSegment(Graphics2D g2, int x1, int y1, int x2, int y2,
                                  boolean horizontal) {
        // Road body
        g2.setColor(COLOR_ROAD);
        if (horizontal) {
            g2.fillRect(x1, y1 - ROAD_HALF_WIDTH, x2 - x1, ROAD_HALF_WIDTH * 2);
        } else {
            g2.fillRect(x1 - ROAD_HALF_WIDTH, y1, ROAD_HALF_WIDTH * 2, y2 - y1);
        }

        // Edge lines
        g2.setColor(COLOR_ROAD_EDGE);
        g2.setStroke(new BasicStroke(1.5f));
        if (horizontal) {
            g2.drawLine(x1, y1 - ROAD_HALF_WIDTH, x2, y1 - ROAD_HALF_WIDTH);
            g2.drawLine(x1, y1 + ROAD_HALF_WIDTH, x2, y1 + ROAD_HALF_WIDTH);
        } else {
            g2.drawLine(x1 - ROAD_HALF_WIDTH, y1, x1 - ROAD_HALF_WIDTH, y2);
            g2.drawLine(x1 + ROAD_HALF_WIDTH, y1, x1 + ROAD_HALF_WIDTH, y2);
        }

        // Dashed center divider
        float[] dashPattern = {8f, 6f};
        g2.setColor(COLOR_CENTER_LINE);
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10f, dashPattern, 0f));
        g2.drawLine(x1, y1, x2, y2);

        // Reset stroke
        g2.setStroke(new BasicStroke(1f));
    }

    // -----------------------------------------------------------------------
    // Intersection drawing
    // -----------------------------------------------------------------------

    /**
     * drawIntersections - paints a colored circle at each grid intersection.
     * The fill color reflects the traffic light state (red/yellow/green).
     * A small (row,col) label is painted below each node.
     *
     * @param g2 (Graphics2D)
     */
    private void drawIntersections(Graphics2D g2) {
        ArrayList<TrafficLight> lights = TrafficAnalysisGUI.getTrafficSignals();
        int size = GridPosition.GRID_SIZE;
        int r2   = NODE_DIAMETER / 2;

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int px = GridPosition.GRID_OFFSET + c * GridPosition.CELL_SIZE;
                int py = GridPosition.GRID_OFFSET + r * GridPosition.CELL_SIZE;

                // Look up this intersection's light color
                Color nodeColor = COLOR_UNKNOWN;
                for (TrafficLight tl : lights) {
                    GridPosition gp = tl.getGridPos();
                    if (gp.row == r && gp.col == c) {
                        String col2 = tl.getColor();
                        if ("green".equalsIgnoreCase(col2))       nodeColor = COLOR_GREEN;
                        else if ("yellow".equalsIgnoreCase(col2)) nodeColor = COLOR_YELLOW;
                        else if ("red".equalsIgnoreCase(col2))    nodeColor = COLOR_RED;
                        break;
                    }
                }

                // Fill and border circle
                g2.setColor(nodeColor);
                g2.fillOval(px - r2, py - r2, NODE_DIAMETER, NODE_DIAMETER);
                g2.setColor(COLOR_NODE_BORDER);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(px - r2, py - r2, NODE_DIAMETER, NODE_DIAMETER);
                g2.setStroke(new BasicStroke(1f));

                // Label
                g2.setColor(COLOR_LABEL);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                String label = r + "," + c;
                FontMetrics fm = g2.getFontMetrics();
                int lw = fm.stringWidth(label);
                g2.drawString(label, px - lw / 2, py + r2 + 11);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Car drawing
    // -----------------------------------------------------------------------

    /**
     * drawCars - paints each car as a colored rectangle with a directional
     * arrow, positioned using both the grid intersection center and the car's
     * sub-cell pixel offset (for smooth inter-intersection animation).
     *
     * Cars traveling in opposite directions are offset perpendicular to the
     * road center line to simulate two-lane traffic.
     *
     * @param g2 (Graphics2D)
     */
    private void drawCars(Graphics2D g2) {
        // Snapshot the list to avoid ConcurrentModificationException
        List<Car> snapshot;
        synchronized (cars) {
            snapshot = new ArrayList<>(cars);
        }

        for (Car car : snapshot) {
            // Base intersection pixel center
            int baseX = GridPosition.GRID_OFFSET + car.gridCol * GridPosition.CELL_SIZE;
            int baseY = GridPosition.GRID_OFFSET + car.gridRow * GridPosition.CELL_SIZE;

            // Apply sub-cell offset (set by CarMovement during animation steps)
            int drawX = baseX + car.pixelOffsetX;
            int drawY = baseY + car.pixelOffsetY;

            // Perpendicular lane offset keeps opposing-direction cars from overlapping
            int laneOffset = 5;   // pixels away from road center line
            switch (car.heading) {
                case EAST:  drawY -= laneOffset; break;
                case WEST:  drawY += laneOffset; break;
                case SOUTH: drawX += laneOffset; break;
                case NORTH: drawX -= laneOffset; break;
            }

            drawCar(g2, car, drawX, drawY);
        }
    }

    /**
     * drawCar - renders a single car as a rotated rectangle with an
     * embedded directional arrow.
     *
     * The rectangle is always painted as if heading EAST, then rotated
     * via AffineTransform to match the car's actual heading.
     *
     * @param g2    (Graphics2D)
     * @param car   (Car) the car to draw
     * @param cx    (int) pixel X center of the car
     * @param cy    (int) pixel Y center of the car
     */
    private void drawCar(Graphics2D g2, Car car, int cx, int cy) {
        // Rotation angle based on heading (0 = East, clockwise positive)
        double angle;
        switch (car.heading) {
            case EAST:  angle = 0;                break;
            case SOUTH: angle = Math.PI / 2;      break;
            case WEST:  angle = Math.PI;           break;
            case NORTH: angle = -Math.PI / 2;     break;
            default:    angle = 0;
        }

        // Save transform, rotate around car center
        AffineTransform saved = g2.getTransform();
        g2.rotate(angle, cx, cy);

        // Car body rectangle
        int halfW = CAR_WIDTH / 2;
        int halfH = CAR_HEIGHT / 2;
        g2.setColor(car.carColor);
        g2.fillRoundRect(cx - halfW, cy - halfH, CAR_WIDTH, CAR_HEIGHT, 4, 4);

        // Dark outline
        g2.setColor(car.carColor.darker().darker());
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(cx - halfW, cy - halfH, CAR_WIDTH, CAR_HEIGHT, 4, 4);

        // Windshield highlight (right side of rectangle = front of eastward car)
        g2.setColor(new Color(200, 230, 255, 180));
        g2.fillRect(cx + halfW - 5, cy - halfH + 2, 4, CAR_HEIGHT - 4);

        // Direction arrow (small triangle at the front)
        int[] arrowX = { cx + halfW + 2, cx + halfW + 6, cx + halfW + 2 };
        int[] arrowY = { cy - 3, cy, cy + 3 };
        g2.setColor(Color.ORANGE);
        g2.fillPolygon(arrowX, arrowY, 3);

        // Restore transform
        g2.setTransform(saved);
    }
}
