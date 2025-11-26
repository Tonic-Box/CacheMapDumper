package osrs.dev.collisionmap.roaring;

import org.roaringbitmap.RoaringBitmap;
import osrs.dev.collisionmap.ICollisionMapWriter;

import java.io.*;
import java.util.zip.GZIPOutputStream;

public class RoaringCollisionMapWriter implements ICollisionMapWriter {
    private final RoaringBitmap bitmap;

    public RoaringCollisionMapWriter() {
        this.bitmap = new RoaringBitmap();
    }

    /**
     * Internal set - stores blocking state.
     * blocked=true → set bit, blocked=false → clear bit
     */
    private synchronized void setBlocked(int index, boolean blocked) {
        if (blocked){
            bitmap.add(index);
        } else {
            bitmap.remove(index);
        }
    }

    @Override
    public void setPathableNorth(int x, int y, int plane, boolean pathable) {
        // Invert: pathable=true means NOT blocked, so set bit to false
        setBlocked(RoaringCollisionMap.INDEXER.packToBitmapIndex(x, y, plane, RoaringCollisionMap.NORTH_DATA_BIT_POS), !pathable);
    }

    @Override
    public void setPathableEast(int x, int y, int plane, boolean pathable) {
        setBlocked(RoaringCollisionMap.INDEXER.packToBitmapIndex(x, y, plane, RoaringCollisionMap.EAST_DATA_BIT_POS), !pathable);
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
