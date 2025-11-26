package osrs.dev.collisionmap;

import osrs.dev.tiledatamap.ITileDataMap;

/**
 * Generic collision map backed by any ITileDataMap implementation.
 * Maps NORTH and EAST direction bits with inverted semantics.
 *
 * Data bit SET = BLOCKED (cannot walk in that direction)
 * Interface returns pathable=true when bit is NOT set
 */
public class CollisionMap {
    static final int NORTH_DATA_BIT_POS = 0;
    static final int EAST_DATA_BIT_POS = 1;

    private final ITileDataMap dataMap;

    public CollisionMap(ITileDataMap dataMap) {
        this.dataMap = dataMap;
    }

    public boolean pathableNorth(int x, int y, int plane) {
        return !dataMap.isDataBitSet(x, y, plane, NORTH_DATA_BIT_POS);
    }

    public boolean pathableEast(int x, int y, int plane) {
        return !dataMap.isDataBitSet(x, y, plane, EAST_DATA_BIT_POS);
    }

    public boolean pathableSouth(int x, int y, int plane) {
        return pathableNorth(x, y - 1, plane);
    }

    public boolean pathableWest(int x, int y, int plane) {
        return pathableEast(x - 1, y, plane);
    }

    public boolean isBlocked(int x, int y, int plane) {
        return !pathableNorth(x, y, plane)
                && !pathableEast(x, y, plane)
                && !pathableSouth(x, y, plane)
                && !pathableWest(x, y, plane);
    }

    /**
     * Returns combined pathability flags for all 8 directions.
     * Bit layout matches osrs.dev.collisionmap.Flags constants:
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
    public byte all(int x, int y, int plane) {
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
