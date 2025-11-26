package osrs.dev.tiledatamap;

import java.io.IOException;

/**
 * Write interface for generic tile data maps.
 * Provides methods to set individual data bits and save to file.
 * Data interpretation is agnostic to the caller.
 */
public interface ITileDataMapWriter {

    /**
     * Sets a data bit at the specified coordinate.
     *
     * @param x            the x coordinate
     * @param y            the y coordinate
     * @param plane        the plane
     * @param dataBitIndex the bit index to set
     */
    void setDataBit(int x, int y, int plane, int dataBitIndex);

    /**
     * Sets multiple data bits from an integer value.
     * Bits that are set in the value will be set at the coordinate.
     *
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param plane the plane
     * @param data  the data value whose bits will be set
     */
    default void setAllDataBits(int x, int y, int plane, int data) {
        for (int i = 0; i <= 31; i++) {
            if ((data & (1 << i)) != 0) {
                setDataBit(x, y, plane, i);
            }
        }
    }

    /**
     * Saves the data map to a file.
     * Determines whether to use gzip based on whether the filepath ends with ".gz".
     *
     * @param filePath the file path
     * @throws IOException if an I/O error occurs
     */
    void save(String filePath) throws IOException;
}
