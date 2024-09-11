package osrs.dev.dumper;

import VitaX.services.local.pathfinder.engine.collision.SparseBitSet;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class GlobalCollisionMapWriter {
    private static final int W_FLAG = 1 << 30;
    private final SparseBitSet bitSet;

    public GlobalCollisionMapWriter() {
        this.bitSet = new SparseBitSet();
    }

    private void set(int index, boolean value) {
        bitSet.set(index, value);
    }

    public void northBlocking(short x, short y, byte z, boolean value) {
        int index = (x & 8191) | ((y & 32767) << 13) | (z << 28);
        set(index, value);
    }

    public void eastBlocking(short x, short y, byte z, boolean value) {
        int index = (x & 8191) | ((y & 32767) << 13) | (z << 28) | W_FLAG;
        set(index, value);
    }

    public void southBlocking(short x, short y, byte z, boolean value) {
        northBlocking(x, (short) (y - 1), z, value);
    }

    public void westBlocking(short x, short y, byte z, boolean value) {
        eastBlocking((short) (x - 1), y, z, value);
    }

    public void fullBlocking(short x, short y, byte z, boolean blocking) {
        northBlocking(x, y, z, blocking);
        eastBlocking(x, y, z, blocking);
        southBlocking(x, y, z, blocking);
        westBlocking(x, y, z, blocking);
    }

    public void save(String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(bitSet);
        }
    }
}