package osrs.dev.dumper;

import com.tonic.services.pathfinder.collision.SparseBitSet;

import java.io.*;

/**
 * Writer for tile type data using SparseBitSet storage.
 * Stores 4-bit tile type values per coordinate using 4 separate bit sets.
 */
public class TileTypeMapWriter {
    // Use 4 separate SparseBitSets to avoid negative index issues with bits 30-31
    private final SparseBitSet[] bitSets = new SparseBitSet[4];

    public TileTypeMapWriter() {
        for (int i = 0; i < 4; i++) {
            bitSets[i] = new SparseBitSet();
        }
    }

    /**
     * Sets the tile type at the specified coordinate.
     * Decomposes the tile type byte into 4 individual bits stored in separate bit sets.
     *
     * @param x    the x coordinate
     * @param y    the y coordinate
     * @param z    the z/plane coordinate
     * @param type the tile type (0-15)
     */
    public void setTileType(int x, int y, int z, byte type) {
        int index = (x & 8191) | ((y & 32767) << 13) | (z << 28);
        // Decompose type byte into 4 individual bits, one per SparseBitSet
        for (int bit = 0; bit < 4; bit++) {
            if ((type & (1 << bit)) != 0) {
                bitSets[bit].set(index);
            }
        }
    }

    /**
     * Saves the tile type map to a file.
     *
     * @param filePath the file path to save to
     * @throws IOException if an I/O error occurs
     */
    public void save(String filePath) throws IOException {
        try (OutputStream os = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(os)) {
            oos.writeObject(bitSets);
        }
    }
}
