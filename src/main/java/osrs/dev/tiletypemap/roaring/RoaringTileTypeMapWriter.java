package osrs.dev.tiletypemap.roaring;

import org.roaringbitmap.RoaringBitmap;
import osrs.dev.dumper.ICoordIndexer;
import osrs.dev.tiletypemap.ITileTypeMapWriter;
import osrs.dev.tiletypemap.sparse.SparseBitSetTileTypeMap;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * Writer for tile type data using RoaringBitmap with type value encoded in upper bits.
 * Uses 4 bits (28-31) to encode type values 0-15.
 */
public class RoaringTileTypeMapWriter implements ITileTypeMapWriter {
    private final RoaringBitmap bitmap;

    public RoaringTileTypeMapWriter() {
        this.bitmap = new RoaringBitmap();
    }

    @Override
    public synchronized void setDataBit(int x, int y, int plane, int dataBitIndex) {
        bitmap.add(RoaringTileTypeMap.INDEXER.packToBitmapIndex(x, y, plane, dataBitIndex));
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
        bitmap.runOptimize();

        try (FileOutputStream fos = new FileOutputStream(filePath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(gzos)) {

            bitmap.serialize(dos);
        }
    }

    /**
     * Saves the tile type map without GZIP compression.
     *
     * @param filePath path to save the tile type map
     * @throws IOException if saving fails
     */
    public void saveWithoutGzip(String filePath) throws IOException {
        bitmap.runOptimize();

        try (FileOutputStream fos = new FileOutputStream(filePath);
             DataOutputStream dos = new DataOutputStream(fos)) {

            bitmap.serialize(dos);
        }
    }
}
