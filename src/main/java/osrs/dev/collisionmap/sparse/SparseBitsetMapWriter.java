package osrs.dev.collisionmap.sparse;

import VitaX.services.local.pathfinder.engine.collision.SparseBitSet;
import osrs.dev.collisionmap.ICollisionMapWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * SparseBitSet-based collision map writer with configurable coordinate packing.
 *
 * Storage convention: bit SET = BLOCKED (cannot walk)
 * This is inverted from the interface semantics where pathable=true means can walk.
 */
public class SparseBitsetMapWriter implements ICollisionMapWriter {
    private final SparseBitSet bitSet;


    public SparseBitsetMapWriter() {
        this.bitSet = new SparseBitSet();
    }

    /**
     * Internal set - stores blocking state.
     * blocked=true → set bit, blocked=false → clear bit
     */
    private synchronized void setBlocked(int index, boolean blocked) {
        bitSet.set(index, blocked);
    }

    // ==================== ICollisionMapWriter interface ====================

    @Override
    public void setPathableNorth(int x, int y, int plane, boolean pathable) {
        // Invert: pathable=true means NOT blocked, so set bit to false
        setBlocked(SparseBitSetCollisionMap.INDEXER.packToBitmapIndex(x, y, plane, SparseBitSetCollisionMap.NORTH_DATA_BIT_POS), !pathable);
    }

    @Override
    public void setPathableEast(int x, int y, int plane, boolean pathable) {
        setBlocked(SparseBitSetCollisionMap.INDEXER.packToBitmapIndex(x, y, plane, SparseBitSetCollisionMap.EAST_DATA_BIT_POS), !pathable);
    }

    @Override
    public void save(String filePath) throws IOException {
        if (filePath.endsWith(".gz")) {
            saveGzipped(filePath);
        } else {
            saveWithoutGzip(filePath);
        }
    }

    /**
     * Saves the collision map with GZIP compression.
     *
     * @param filePath path to save the collision map
     * @throws IOException if saving fails
     */
    public void saveGzipped(String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(gzos)) {
            oos.writeObject(bitSet);
        }
    }

    /**
     * Saves the collision map without GZIP compression.
     *
     * @param filePath path to save the collision map
     * @throws IOException if saving fails
     */
    public void saveWithoutGzip(String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(bitSet);
        }
    }
}
