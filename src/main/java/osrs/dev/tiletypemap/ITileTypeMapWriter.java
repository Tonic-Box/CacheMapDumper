package osrs.dev.tiletypemap;

import osrs.dev.dumper.ICoordIndexer;
import osrs.dev.tiletypemap.roaring.RoaringTileTypeMap;

import java.io.IOException;

/**
 * Write interface for tile type maps.
 * Provides methods to set tile types and save to file.
 */
public interface ITileTypeMapWriter {
    void setDataBit(int x, int y, int plane, int dataBitIndex);

    /**
     * Sets the tile type for a coordinate.
     * The type value (0-15) is encoded as 4 binary bits in the map.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param plane the plane
     * @param type the tile type value (0-15)
     */
    default void setTileType(int x, int y, int plane, byte type){
        if ((type & 0b0001) != 0) setDataBit(x, y, plane, 0);
        if ((type & 0b0010) != 0) setDataBit(x, y, plane, 1);
        if ((type & 0b0100) != 0) setDataBit(x, y, plane, 2);
        if ((type & 0b1000) != 0) setDataBit(x, y, plane, 3);
    }

    /**
     * Saves the tile type map to a file.
     * Determines whether to use gzip based on whether the filepath ends with ".gz".
     *
     * @param filePath the file path
     * @throws IOException if an I/O error occurs
     */
    void save(String filePath) throws IOException;
}
