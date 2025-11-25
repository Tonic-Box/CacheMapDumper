package osrs.dev.dumper;

import VitaX.services.local.pathfinder.engine.collision.SparseBitSet;
import osrs.dev.collision.ICollisionMapWriter;
import osrs.dev.collision.ICoordPacker;
import osrs.dev.collision.ConfigurableCoordPacker;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * SparseBitSet-based collision map writer with configurable coordinate packing.
 *
 * Storage convention: bit SET = BLOCKED (cannot walk)
 * This is inverted from the interface semantics where pathable=true means can walk.
 */
public class SparseBitsetMapWriter implements ICollisionMapWriter {
    private final SparseBitSet bitSet;
    private final ICoordPacker packing;

    public SparseBitsetMapWriter() {
        this(ConfigurableCoordPacker.JAGEX_PACKING);
    }

    public SparseBitsetMapWriter(ICoordPacker packing) {
        this.bitSet = new SparseBitSet();
        this.packing = packing;
    }

    /**
     * Internal set - stores blocking state.
     * blocked=true → set bit, blocked=false → clear bit
     */
    private void setBlocked(int index, boolean blocked) {
        bitSet.set(index, blocked);
    }

    // ==================== ICollisionMapWriter interface ====================

    @Override
    public void setPathableNorth(int x, int y, int plane, boolean pathable) {
        // Invert: pathable=true means NOT blocked, so set bit to false
        setBlocked(packing.pack(x, y, plane), !pathable);
    }

    @Override
    public void setPathableEast(int x, int y, int plane, boolean pathable) {
        setBlocked(packing.packEast(x, y, plane), !pathable);
    }

    // Note: setPathableSouth, setPathableWest, setAllDirections use default implementations

    @Override
    public void save(String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(bitSet);
        }
    }

    // ==================== Legacy methods for backward compatibility ====================

    /**
     * Legacy method: sets blocking state for north direction.
     * blocking=true means cannot walk north (sets bit)
     */
    public void northBlocking(short x, short y, byte z, boolean blocking) {
        setBlocked(packing.pack(x, y, z), blocking);
    }

    /**
     * Legacy method: sets blocking state for east direction.
     * blocking=true means cannot walk east (sets bit)
     */
    public void eastBlocking(short x, short y, byte z, boolean blocking) {
        setBlocked(packing.packEast(x, y, z), blocking);
    }

    /**
     * Legacy method: sets blocking state for south direction.
     */
    public void southBlocking(short x, short y, byte z, boolean blocking) {
        northBlocking(x, (short) (y - 1), z, blocking);
    }

    /**
     * Legacy method: sets blocking state for west direction.
     */
    public void westBlocking(short x, short y, byte z, boolean blocking) {
        eastBlocking((short) (x - 1), y, z, blocking);
    }

    /**
     * Legacy method: sets blocking state for all four directions.
     * blocking=true means cannot walk in any direction (sets all bits)
     */
    public void fullBlocking(short x, short y, byte z, boolean blocking) {
        northBlocking(x, y, z, blocking);
        eastBlocking(x, y, z, blocking);
        southBlocking(x, y, z, blocking);
        westBlocking(x, y, z, blocking);
    }
}
