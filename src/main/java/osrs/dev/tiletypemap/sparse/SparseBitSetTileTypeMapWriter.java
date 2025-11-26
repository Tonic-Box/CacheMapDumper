package osrs.dev.tiletypemap.sparse;

import VitaX.services.local.pathfinder.engine.collision.SparseBitSet;
import osrs.dev.tiletypemap.ITileTypeMapWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * SparseBitSet-based tile type map writer with 4-bit type encoding.
 * Uses bits 27-30 to encode type values 0-15.
 * Does not support planes other than 0.
 */
public class SparseBitSetTileTypeMapWriter implements ITileTypeMapWriter {
    private final SparseBitSet bitSet;

    public SparseBitSetTileTypeMapWriter() {
        this.bitSet = new SparseBitSet();
    }

    @Override
    public synchronized void setDataBit(int x, int y, int plane, int dataBitIndex) {
        bitSet.set(SparseBitSetTileTypeMap.INDEXER.packToBitmapIndex(x, y, plane, dataBitIndex));
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
     * Saves the tile type map with GZIP compression.
     *
     * @param filePath path to save the tile type map
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
     * Saves the tile type map without GZIP compression.
     *
     * @param filePath path to save the tile type map
     * @throws IOException if saving fails
     */
    public void saveWithoutGzip(String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(bitSet);
        }
    }
}
