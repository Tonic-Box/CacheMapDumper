package osrs.dev.collision;

import org.roaringbitmap.RoaringBitmap;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * Writer for RoaringBitmap-based collision maps with configurable coordinate packing.
 *
 * Bit SET = BLOCKED (cannot walk in that direction)
 */
public class RoaringCollisionMapWriter implements ICollisionMapWriter {

    private final RoaringBitmap bitmap;
    private final ICoordPacking packing;

    public RoaringCollisionMapWriter() {
        this(ConfigurablePacking.JAGEX_PACKING);
    }

    public RoaringCollisionMapWriter(ICoordPacking packing) {
        this.bitmap = new RoaringBitmap();
        this.packing = packing;
    }

    @Override
    public synchronized void setPathableNorth(int x, int y, int plane, boolean pathable) {
        int index = packing.pack(x, y, plane);
        if (pathable) {
            bitmap.remove(index);
        } else {
            bitmap.add(index);
        }
    }

    @Override
    public synchronized void setPathableEast(int x, int y, int plane, boolean pathable) {
        int index = packing.packEast(x, y, plane);
        if (pathable) {
            bitmap.remove(index);
        } else {
            bitmap.add(index);
        }
    }

    @Override
    public void save(String filePath) throws IOException {
        bitmap.runOptimize();

        try (FileOutputStream fos = new FileOutputStream(filePath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(gzos)) {

            bitmap.serialize(dos);
        }
    }
}
