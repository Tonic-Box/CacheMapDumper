package osrs.dev.util;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a point in the world.
 */
@Data
@AllArgsConstructor
public class WorldPoint {
    private int x;
    private int y;
    private int plane;

    /**
     * Moves the point north by n tiles.
     * @param n The number of tiles to move.
     */
    public void north(int n)
    {
        y += n;
    }

    /**
     * Moves the point south by n tiles.
     * @param n The number of tiles to move.
     */
    public void south(int n)
    {
        y -= n;
    }

    /**
     * Moves the point east by n tiles.
     * @param n The number of tiles to move.
     */
    public void east(int n)
    {
        x += n;
    }

    /**
     * Moves the point west by n tiles.
     * @param n The number of tiles to move.
     */
    public void west(int n)
    {
        x -= n;
    }
}