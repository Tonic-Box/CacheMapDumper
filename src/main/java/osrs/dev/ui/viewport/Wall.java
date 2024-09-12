package osrs.dev.ui.viewport;

import lombok.Getter;

/**
 * Represents a wall in the game world.
 */
@Getter
public class Wall {
    private final int cellX;
    private final int cellY;
    private final Direction direction;

    /**
     * Creates a new wall.
     *
     * @param cellX      the x-coordinate of the wall
     * @param cellY      the y-coordinate of the wall
     * @param direction  the direction of the wall
     */
    public Wall(int cellX, int cellY, Direction direction) {
        this.cellX = cellX;
        this.cellY = cellY;
        this.direction = direction;
    }

    /**
     * Represents the direction of the wall.
     */
    public enum Direction {
        WEST, EAST, NORTH, SOUTH, DIAGONAL_NORTH_EAST, DIAGONAL_NORTH_WEST
    }
}