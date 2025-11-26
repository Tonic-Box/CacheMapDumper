package osrs.dev.tiletypemap;

import osrs.dev.dumper.ICoordIndexer;

/**
 * Read interface for tile type maps.
 * Provides methods to query tile type information.
 */
public interface ITileTypeMap {

    ICoordIndexer getIndexer();

    boolean isDataBitSet(int x, int y, int plane, int dataBitIndex);

    /**
     * Gets the tile type for a coordinate.
     *
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param plane the plane
     * @return the tile type value (0-15), 0 if no type set
     */
    default byte getTileType(int x, int y, int plane) {
        int data = 0;
        for (int i = 0; i <= getIndexer().getMaxDataBitIndex(); i++) {
            if (isDataBitSet(x, y, plane, i)) {
                data |= (1 << i);
            }
        }
        return (byte) data;
    }


    /**
     * Checks if a tile is any type of water.
     *
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param plane the plane
     * @return true if the tile is water
     */
    default boolean isWater(int x, int y, int plane) {
        return getTileType(x, y, plane) >= TileType.WATER;
    }

    /**
     * Checks if a tile has a specific type.
     *
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param plane the plane
     * @param type  the type to check for
     * @return true if the tile has the specified type
     */
    default boolean hasType(int x, int y, int plane, byte type) {
        return getTileType(x, y, plane) == type;
    }
}
