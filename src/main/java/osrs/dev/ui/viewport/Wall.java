package osrs.dev.ui.viewport;

import lombok.Getter;

@Getter
public class Wall {
    private final int cellX;
    private final int cellY;
    private final Direction direction;

    public Wall(int cellX, int cellY, Direction direction) {
        this.cellX = cellX;
        this.cellY = cellY;
        this.direction = direction;
    }

    public enum Direction {
        WEST, EAST, NORTH, SOUTH, DIAGONAL_NORTH_EAST, DIAGONAL_NORTH_WEST
    }
}