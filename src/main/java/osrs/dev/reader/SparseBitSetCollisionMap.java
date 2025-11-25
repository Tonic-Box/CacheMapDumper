package osrs.dev.reader;

import VitaX.services.local.pathfinder.engine.collision.SparseBitSet;
import osrs.dev.collision.ICollisionMap;
import osrs.dev.collision.ICoordPacker;
import osrs.dev.collision.ConfigurableCoordPacker;

import java.io.*;

/**
 * SparseBitSet-based collision map with configurable coordinate packing.
 * Maintains backwards compatibility with existing .dat files.
 *
 * Storage convention: bit SET = BLOCKED (cannot walk)
 */
public class SparseBitSetCollisionMap implements ICollisionMap {
    private final SparseBitSet bitSet;
    private final ICoordPacker packing;

    /**
     * Creates a new collision map with default legacy packing.
     *
     * @param bitSet The bit set.
     */
    private SparseBitSetCollisionMap(SparseBitSet bitSet) {
        this(bitSet, ConfigurableCoordPacker.JAGEX_PACKING);
    }

    /**
     * Creates a new collision map with specified packing.
     *
     * @param bitSet The bit set.
     * @param packing The coordinate packing to use.
     */
    private SparseBitSetCollisionMap(SparseBitSet bitSet, ICoordPacker packing) {
        this.bitSet = bitSet;
        this.packing = packing;
    }

    /**
     * Internal check - bit SET means BLOCKED in storage.
     * Returns true if pathable (bit NOT set).
     */
    private boolean isPathable(int index) {
        return !bitSet.get(index);
    }

    /**
     * Legacy method: returns 0 if blocked (bit set), 1 if pathable (bit not set).
     */
    private byte get(int index) {
        return (byte) (bitSet.get(index) ? 0 : 1);
    }

    // ==================== ICollisionMap interface ====================

    @Override
    public boolean pathableNorth(int x, int y, int plane) {
        return isPathable(packing.pack(x, y, plane));
    }

    @Override
    public boolean pathableEast(int x, int y, int plane) {
        return isPathable(packing.packEast(x, y, plane));
    }

    // Note: pathableSouth and pathableWest use default implementations from interface

    @Override
    public byte all(int x, int y, int plane) {
        // Use the interface's default implementation
        return ICollisionMap.super.all(x, y, plane);
    }

    // ==================== Legacy methods for backward compatibility ====================

    /**
     * Legacy all() method with short/byte parameters.
     * Used by existing ViewPort code.
     */
    public byte all(short x, short y, byte z) {
        return all((int) x, (int) y, (int) z);
    }

    /**
     * Legacy north check returning byte.
     */
    public byte n(short x, short y, byte z) {
        return get(packing.pack(x, y, z));
    }

    /**
     * Legacy east check returning byte.
     */
    public byte e(short x, short y, byte z) {
        return get(packing.packEast(x, y, z));
    }

    /**
     * Legacy south check returning byte.
     */
    public byte s(short x, short y, byte z) {
        return n(x, (short) (y - 1), z);
    }

    /**
     * Legacy west check returning byte.
     */
    public byte w(short x, short y, byte z) {
        return e((short) (x - 1), y, z);
    }

    // ==================== Static factory ====================

    /**
     * Loads a collision map from a file with default legacy packing.
     *
     * @param filePath The file path.
     * @return The collision map.
     * @throws IOException            On file read error.
     * @throws ClassNotFoundException On class not found.
     */
    public static SparseBitSetCollisionMap load(String filePath) throws IOException, ClassNotFoundException {
        return load(filePath, ConfigurableCoordPacker.JAGEX_PACKING);
    }

    /**
     * Loads a collision map from a file with specified packing.
     *
     * @param filePath The file path.
     * @param packing The coordinate packing to use.
     * @return The collision map.
     * @throws IOException            On file read error.
     * @throws ClassNotFoundException On class not found.
     */
    public static SparseBitSetCollisionMap load(String filePath, ICoordPacker packing) throws IOException, ClassNotFoundException {
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            System.err.println("File not found: " + filePath);
            return null;
        }

        try (InputStream is = new FileInputStream(file);
             ObjectInputStream objectInputStream = new ObjectInputStream(is)) {
            return new SparseBitSetCollisionMap((SparseBitSet) objectInputStream.readObject(), packing);
        }
    }
}
