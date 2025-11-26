package osrs.dev.tiletypemap.roaring;

import org.roaringbitmap.RoaringBitmap;
import osrs.dev.dumper.ConfigurableCoordPacker;
import osrs.dev.dumper.ICoordPacker;
import osrs.dev.tiletypemap.ITileTypeMapWriter;
import osrs.dev.tiletypemap.TileType;
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
    public synchronized void setTileType(int x, int y, int plane, byte type) {
        int packed = RoaringTileTypeMap.packing.pack(x, y, plane);
        if ((type & 0b0001) != 0) bitmap.add(packed | SparseBitSetTileTypeMap.SPARSE_TILE_TYPE_BIT_0);
        if ((type & 0b0010) != 0) bitmap.add(packed | SparseBitSetTileTypeMap.SPARSE_TILE_TYPE_BIT_1);
        if ((type & 0b0100) != 0) bitmap.add(packed | SparseBitSetTileTypeMap.SPARSE_TILE_TYPE_BIT_2);
        if ((type & 0b1000) != 0) bitmap.add(packed | SparseBitSetTileTypeMap.SPARSE_TILE_TYPE_BIT_3);
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
