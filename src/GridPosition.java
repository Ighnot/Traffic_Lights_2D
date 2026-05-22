/**
 * GridPosition.java
 * @author John Leckie (original), refactored for 2D grid
 * CMSC335, Dec 2023, Project 3 — 2D Grid Edition
 *
 * Immutable value class representing a cell position on the 6x6
 * intersection grid. Row and column are both 0-based (0..5).
 * Pixel coordinates for rendering are computed here as well.
 */
public class GridPosition {

    /** Number of rows and columns in the intersection grid. */
    public static final int GRID_SIZE = 4;

    /** Pixel spacing between intersections on the canvas. */
    public static final int CELL_SIZE = 100;

    /** Pixel offset so the grid doesn't start at the canvas edge. */
    public static final int GRID_OFFSET = 50;

    public final int row;
    public final int col;

    /**
     * GridPosition - parameterized constructor.
     *
     * @param row (int) 0-based row index
     * @param col (int) 0-based column index
     */
    public GridPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * pixelX - computes the canvas X pixel coordinate for this grid column.
     * Used by the GridCanvas when painting intersection nodes and cars.
     *
     * @return int pixel X coordinate of the intersection center
     */
    public int pixelX() {
        return GRID_OFFSET + col * CELL_SIZE;
    }

    /**
     * pixelY - computes the canvas Y pixel coordinate for this grid row.
     * Used by the GridCanvas when painting intersection nodes and cars.
     *
     * @return int pixel Y coordinate of the intersection center
     */
    public int pixelY() {
        return GRID_OFFSET + row * CELL_SIZE;
    }

    /**
     * isValid - checks whether this position lies within the 6x6 grid.
     *
     * @return true if both row and col are within [0, GRID_SIZE-1]
     */
    public boolean isValid() {
        return row >= 0 && row < GRID_SIZE && col >= 0 && col < GRID_SIZE;
    }

    /**
     * neighbor - returns the adjacent GridPosition in a given direction.
     * The caller should check isValid() on the result before using it,
     * since moving off the edge of the grid produces an invalid position.
     *
     * @param dir Direction to step in
     * @return GridPosition one step in that direction
     */
    public GridPosition neighbor(Direction dir) {
        switch (dir) {
            case NORTH: return new GridPosition(row - 1, col);
            case SOUTH: return new GridPosition(row + 1, col);
            case EAST:  return new GridPosition(row, col + 1);
            case WEST:  return new GridPosition(row, col - 1);
            default:    return this;
        }
    }

    /**
     * equals - two GridPositions are equal if their row and col match.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GridPosition)) return false;
        GridPosition other = (GridPosition) o;
        return this.row == other.row && this.col == other.col;
    }

    @Override
    public int hashCode() {
        return 31 * row + col;
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }
}
