package osrs.dev.collisionmap.roaring;

import org.roaringbitmap.RoaringBitmap;
import osrs.dev.dumper.ConfigurableCoordPacker;
import osrs.dev.collisionmap.ICollisionMap;
import osrs.dev.dumper.ICoordPacker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * RoaringBitmap-based collision map with configurable coordinate packing.
 * Uses native RoaringBitmap serialization (may be gzipped).
 *
 * Bit SET = BLOCKED (cannot walk in that direction)
 */
public class RoaringCollisionMap implements ICollisionMap {
    public static final ICoordPacker packing = ConfigurableCoordPacker.JAGEX_PACKING;

    private final RoaringBitmap bitmap;

    public RoaringCollisionMap(RoaringBitmap bitmap) {
        this.bitmap = bitmap;
    }


    @Override
    public boolean pathableNorth(int x, int y, int plane) {
        return !bitmap.contains(packing.pack(x, y, plane));
    }

    @Override
    public boolean pathableEast(int x, int y, int plane) {
        return !bitmap.contains(packing.packEast(x, y, plane));
    }


    /**
     * Loads from RoaringBitmap native format.
     * The input stream should already be decompressed if it was gzipped.
     *
     * @param inputStream input stream containing RoaringBitmap data
     * @return loaded collision map
     * @throws IOException if an I/O error occurs
     */
    public static RoaringCollisionMap load(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        RoaringBitmap bitmap = new RoaringBitmap();
        bitmap.deserialize(ByteBuffer.wrap(bytes));
        bitmap.runOptimize();
        return new RoaringCollisionMap(bitmap);
    }
}
