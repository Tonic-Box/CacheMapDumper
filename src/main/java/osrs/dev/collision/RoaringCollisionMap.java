package osrs.dev.collision;

import org.roaringbitmap.RoaringBitmap;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * RoaringBitmap-based collision map with configurable coordinate packing.
 * Uses GZIP-compressed native RoaringBitmap serialization.
 *
 * Bit SET = PATHABLE (can walk in that direction)
 */
public class RoaringCollisionMap implements ICollisionMap {

    private final RoaringBitmap bitmap;
    private final ICoordPacking packing;

    public RoaringCollisionMap() {
        this(new RoaringBitmap(), ConfigurablePacking.JAGEX_PACKING);
    }

    public RoaringCollisionMap(RoaringBitmap bitmap) {
        this(bitmap, ConfigurablePacking.JAGEX_PACKING);
    }

    public RoaringCollisionMap(RoaringBitmap bitmap, ICoordPacking packing) {
        this.bitmap = bitmap;
        this.packing = packing;
    }

    /**
     * Package-private access for writer.
     */
    RoaringBitmap getBitmap() {
        return bitmap;
    }

    @Override
    public boolean pathableNorth(int x, int y, int plane) {
        return bitmap.contains(packing.pack(x, y, plane));
    }

    @Override
    public boolean pathableEast(int x, int y, int plane) {
        return bitmap.contains(packing.packEast(x, y, plane));
    }

    /**
     * Loads from GZIP-compressed RoaringBitmap native format with default packing.
     *
     * @param filePath path to the collision map file
     * @return loaded collision map, or null if file not found
     * @throws IOException if an I/O error occurs
     */
    public static RoaringCollisionMap load(String filePath) throws IOException {
        return load(filePath, ConfigurablePacking.JAGEX_PACKING);
    }

    /**
     * Loads from GZIP-compressed RoaringBitmap native format.
     * Compatible with GlobalCollisionMap serialization.
     *
     * @param filePath path to the collision map file
     * @param packing the coordinate packing to use
     * @return loaded collision map, or null if file not found
     * @throws IOException if an I/O error occurs
     */
    public static RoaringCollisionMap load(String filePath, ICoordPacking packing) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("File not found: " + filePath);
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file);
             GZIPInputStream gzis = new GZIPInputStream(fis)) {

            byte[] bytes = gzis.readAllBytes();
            RoaringBitmap bitmap = new RoaringBitmap();
            bitmap.deserialize(ByteBuffer.wrap(bytes));
            bitmap.runOptimize();
            return new RoaringCollisionMap(bitmap, packing);
        }
    }

    /**
     * Saves using GZIP-compressed RoaringBitmap native format.
     * Compatible with GlobalCollisionMap serialization.
     *
     * @param filePath path to save to
     * @throws IOException if an I/O error occurs
     */
    public void save(String filePath) throws IOException {
        bitmap.runOptimize();

        try (FileOutputStream fos = new FileOutputStream(filePath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(gzos)) {

            bitmap.serialize(dos);
        }
    }
}
