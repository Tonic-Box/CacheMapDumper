package osrs.dev.tiledatamap;

import osrs.dev.dumper.ICoordIndexer;

/**
 * Read interface for generic tile data maps.
 * Provides methods to query individual data bits at tile coordinates.
 * Data interpretation is agnostic to the caller.
 */
public interface ITileDataMap {

    ICoordIndexer getIndexer();

    /**
     * Checks if a data bit is set at the specified coordinate.
     *
     * @param x           the x coordinate
     * @param y           the y coordinate
     * @param plane       the plane
     * @param dataBitIndex the bit index to check
     * @return true if the bit is set, false otherwise
     */
    boolean isDataBitSet(int x, int y, int plane, int dataBitIndex);

    /**
     * Gets all data bits as an integer for the specified coordinate.
     *
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param plane the plane
     * @return the data value with bits set according to the stored data
     */
    default byte getAllDataBits(int x, int y, int plane) {
        int data = 0;
        for (int i = 0; i <= getIndexer().getMaxDataBitIndex(); i++) {
            if (isDataBitSet(x, y, plane, i)) {
                data |= (1 << i);
            }
        }
        return (byte) data;
    }
}
