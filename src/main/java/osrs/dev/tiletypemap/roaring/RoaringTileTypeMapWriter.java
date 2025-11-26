package osrs.dev.tiletypemap.roaring;

import org.roaringbitmap.RoaringBitmap;
import osrs.dev.dumper.ConfigurableCoordPacker;
import osrs.dev.dumper.ICoordPacker;
import osrs.dev.tiletypemap.ITileTypeMapWriter;
import osrs.dev.tiletypemap.TileType;

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
    private static final ICoordPacker packing = ConfigurableCoordPacker.COMPACT_13BIT_PACKING;

    public RoaringTileTypeMapWriter() {
        this.bitmap = new RoaringBitmap();
    }

    @Override
    public synchronized void setTileType(int x, int y, int plane, int type) {
        int packed = packing.pack(x, y, plane);
        if ((type & 1) != 0) bitmap.add(packed | TileType.TILE_TYPE_BIT_0);
        if ((type & 2) != 0) bitmap.add(packed | TileType.TILE_TYPE_BIT_1);
        if ((type & 4) != 0) bitmap.add(packed | TileType.TILE_TYPE_BIT_2);
        if ((type & 8) != 0) bitmap.add(packed | TileType.TILE_TYPE_BIT_3);
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
