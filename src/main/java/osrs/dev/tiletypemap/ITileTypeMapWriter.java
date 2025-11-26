package osrs.dev.tiletypemap;

import java.io.IOException;

/**
 * Write interface for tile type maps.
 * Provides methods to set tile types and save to file.
 */
public interface ITileTypeMapWriter {

    /**
     * Sets the tile type for a coordinate.
     * The type value (0-15) is encoded as 4 binary bits in the upper coordinate bits.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param plane the plane
     * @param type the tile type value (0-15)
     */
    void setTileType(int x, int y, int plane, byte type);

    /**
     * Saves the tile type map to a file.
     * Determines whether to use gzip based on whether the filepath ends with ".gz".
     *
     * @param filePath the file path
     * @throws IOException if an I/O error occurs
     */
    void save(String filePath) throws IOException;
}
