package osrs.dev.collisionmap;

/**
 * Interface for reading collision map data.
 * All pathable* methods return true if movement in that direction is possible.
 */
public interface ICollisionMap {

    /**
     * Checks if north movement is pathable from the given tile.
     * @return true if movement north is allowed
     */
    boolean pathableNorth(int x, int y, int plane);

    /**
     * Checks if east movement is pathable from the given tile.
     * @return true if movement east is allowed
     */
    boolean pathableEast(int x, int y, int plane);

    /**
     * Checks if south movement is pathable from the given tile.
     * Default implementation delegates to north of the tile below.
     * @return true if movement south is allowed
     */
    default boolean pathableSouth(int x, int y, int plane) {
        return pathableNorth(x, y - 1, plane);
    }

    /**
     * Checks if west movement is pathable from the given tile.
     * Default implementation delegates to east of the tile to the west.
     * @return true if movement west is allowed
     */
    default boolean pathableWest(int x, int y, int plane) {
        return pathableEast(x - 1, y, plane);
    }

    /**
     * Checks if tile is completely blocked (no movement in any direction).
     * @return true if tile is blocked in all directions
     */
    default boolean isBlocked(int x, int y, int plane) {
        return !pathableNorth(x, y, plane)
            && !pathableEast(x, y, plane)
            && !pathableSouth(x, y, plane)
            && !pathableWest(x, y, plane);
    }

    /**
     * Returns combined pathability flags for all 8 directions.
     * Bit layout matches osrs.dev.reader.Flags constants:
     * - Bit 0: NORTHWEST
     * - Bit 1: NORTH
     * - Bit 2: NORTHEAST
     * - Bit 3: WEST
     * - Bit 4: EAST
     * - Bit 5: SOUTHWEST
     * - Bit 6: SOUTH
     * - Bit 7: SOUTHEAST
     *
     * @return byte with bits set for each pathable direction
     */
    default byte all(int x, int y, int plane) {
        byte n = pathableNorth(x, y, plane) ? (byte) 1 : 0;
        byte e = pathableEast(x, y, plane) ? (byte) 1 : 0;
        byte s = pathableSouth(x, y, plane) ? (byte) 1 : 0;
        byte w = pathableWest(x, y, plane) ? (byte) 1 : 0;

        if ((n | e | s | w) == 0) {
            return Flags.NONE;
        }

        // Calculate diagonal pathability (requires both adjacent cardinal directions + adjacent tiles)
        byte sw = (byte) (s & w
            & (pathableWest(x, y - 1, plane) ? 1 : 0)
            & (pathableSouth(x - 1, y, plane) ? 1 : 0));
        byte se = (byte) (s & e
            & (pathableEast(x, y - 1, plane) ? 1 : 0)
            & (pathableSouth(x + 1, y, plane) ? 1 : 0));
        byte nw = (byte) (n & w
            & (pathableWest(x, y + 1, plane) ? 1 : 0)
            & (pathableNorth(x - 1, y, plane) ? 1 : 0));
        byte ne = (byte) (n & e
            & (pathableEast(x, y + 1, plane) ? 1 : 0)
            & (pathableNorth(x + 1, y, plane) ? 1 : 0));

        return (byte) (nw | (n << 1) | (ne << 2) | (w << 3) | (e << 4)
                     | (sw << 5) | (s << 6) | (se << 7));
    }
}
