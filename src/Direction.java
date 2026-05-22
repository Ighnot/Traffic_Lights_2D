/**
 * Direction.java
 * @author John Leckie (original), refactored for 2D grid
 * CMSC335, Dec 2023, Project 3 — 2D Grid Edition
 *
 * Represents the four cardinal directions a car can travel on the grid.
 * Used by Car and CarMovement to track heading and compute turns.
 */
public enum Direction {
    NORTH, EAST, SOUTH, WEST;

    /**
     * turnLeft - returns the direction to the left of the current heading.
     * E.g., heading NORTH and turning left yields WEST.
     *
     * @return Direction after a left turn
     */
    public Direction turnLeft() {
        switch (this) {
            case NORTH: return WEST;
            case WEST:  return SOUTH;
            case SOUTH: return EAST;
            case EAST:  return NORTH;
            default:    return this;
        }
    }

    /**
     * turnRight - returns the direction to the right of the current heading.
     * E.g., heading NORTH and turning right yields EAST.
     *
     * @return Direction after a right turn
     */
    public Direction turnRight() {
        switch (this) {
            case NORTH: return EAST;
            case EAST:  return SOUTH;
            case SOUTH: return WEST;
            case WEST:  return NORTH;
            default:    return this;
        }
    }

    /**
     * toArrow - returns a unicode arrow character representing this direction.
     * Used for rendering the car's heading indicator on the grid canvas.
     *
     * @return String arrow character
     */
    public String toArrow() {
        switch (this) {
            case NORTH: return "▲";
            case EAST:  return "▶";
            case SOUTH: return "▼";
            case WEST:  return "◀";
            default:    return "?";
        }
    }
}
