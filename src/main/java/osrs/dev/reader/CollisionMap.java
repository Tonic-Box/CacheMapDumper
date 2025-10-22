package osrs.dev.reader;

import com.tonic.services.pathfinder.collision.SparseBitSet;
import java.io.*;

/**
 * A class that represents a collision map.
 */
public class CollisionMap {
    public static final int W_FLAG = 1 << 30;
    private final SparseBitSet bitSet;

    /**
     * inits a new collision map.
     *
     * @param bitSet The bit set.
     */
    private CollisionMap(SparseBitSet bitSet) {
        this.bitSet = bitSet;
    }

    /**
     * Gets the bit set.
     *
     * @return The bit set.
     */
    private byte get(int index) {
        return (byte)(bitSet.get(index) ? 0 : 1);
    }

    /**
     * Packs the x, y, and z into a single bit-shifted integer.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @return The packed integer.
     */
    public byte all(short x, short y, byte z)
    {
        byte n = n(x,y,z);
        byte e = e(x,y,z);
        byte s = s(x,y,z);
        byte w = w(x,y,z);
        if((n | e | s | w) == 0)
        {
            return 0;
        }
        byte sw = (byte) (s & w & w(x, (short)(y - 1), z) & s((short)(x - 1), y, z));
        byte se = (byte) (s & e & e(x, (short)(y - 1), z) & s((short)(x + 1), y, z));
        byte nw = (byte) (n & w & w(x, (short)(y + 1), z) & n((short)(x - 1), y, z));
        byte ne = (byte) (n & e & e(x, (short)(y + 1), z) & n((short)(x + 1), y, z));

        return (byte) (nw | (n << 1) | (ne << 2) | (w << 3) | (e << 4) | (sw << 5) | (s << 6) | (se << 7));
    }

    /**
     * Gets the collision data for a given coordinate.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @return Collision flag
     */
    public byte n(short x, short y, byte z) {
        return get((x & 8191) | ((y & 32767) << 13) | (z << 28));
    }

    /**
     * Gets the collision data for a given coordinate.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @return Collision flag
     */
    public byte e(short x, short y, byte z) {
        return get((x & 8191) | ((y & 32767) << 13) | (z << 28) | W_FLAG);
    }

    /**
     * Gets the collision data for a given coordinate.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @return Collision flag
     */
    public byte s(short x, short y, byte z) {
        return n(x, (short) (y - 1), z);
    }

    /**
     * Gets the collision data for a given coordinate.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @return Collision flag
     */
    public byte w(short x, short y, byte z) {
        return e((short)(x - 1), y, z);
    }


    /**
     * Loads a collision map from a file.
     * @param filePath The file path.
     * @return The collision map.
     * @throws IOException On file read error.
     * @throws ClassNotFoundException On class not found.
     */
    public static CollisionMap load(String filePath) throws IOException, ClassNotFoundException {
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            System.err.println("File not found: " + filePath);
            return null;
        }

        try (InputStream is = new FileInputStream(file); ObjectInputStream objectInputStream = new ObjectInputStream(is)) {
            return new CollisionMap((SparseBitSet) objectInputStream.readObject());
        }
    }
}