package osrs.dev.collisionmap.roaring;

import org.roaringbitmap.RoaringBitmap;
import osrs.dev.dumper.ConfigurableCoordPacker;
import osrs.dev.dumper.ICoordPacker;
import osrs.dev.collisionmap.ICollisionMapWriter;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * Writer for RoaringBitmap-based collision maps with configurable coordinate packing.
 *
 * Bit SET = BLOCKED (cannot walk in that direction)
 */
public class RoaringCollisionMapWriter implements ICollisionMapWriter {
    private final RoaringBitmap bitmap;

    public RoaringCollisionMapWriter() {
        this.bitmap = new RoaringBitmap();
    }

    @Override
    public synchronized void setPathableNorth(int x, int y, int plane, boolean pathable) {
        int index = RoaringCollisionMap.packing.pack(x, y, plane);
        if (pathable) {
            bitmap.remove(index);
        } else {
            bitmap.add(index);
        }
    }

    @Override
    public synchronized void setPathableEast(int x, int y, int plane, boolean pathable) {
        int index = RoaringCollisionMap.packing.packEast(x, y, plane);
        if (pathable) {
            bitmap.remove(index);
        } else {
            bitmap.add(index);
        }
    }

    @Override
    public void save(String filePath) throws IOException {
        if (filePath.endsWith(".gz")) {
            saveGzipped(filePath);
        } else {
            saveWithoutGzip(filePath);
        }
    }


    public void saveGzipped(String filePath) throws IOException {
        bitmap.runOptimize();

        try (FileOutputStream fos = new FileOutputStream(filePath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(gzos)) {

            bitmap.serialize(dos);
        }
    }

    /**
     * Saves the collision map without GZIP compression.
     *
     * @param filePath path to save the collision map
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
