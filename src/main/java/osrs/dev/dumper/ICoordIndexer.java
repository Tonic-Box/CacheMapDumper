package osrs.dev.dumper;

/**
 * Interface for indexing data associated with coordinates in a bitmap
 */
public interface ICoordIndexer {
    /**
     * Packs x, y, plane coordinates and data bit position
     * into an index to a bitmap to retrieve a bit of data for the coordinate.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param plane the plane/level
     * @return packed integer index
     */
    int packToBitmapIndex(int x, int y, int plane, int dataBitPosition);

    int getMaxDataBitIndex();
}
