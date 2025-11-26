package osrs.dev.tiletypemap.sparse;

import VitaX.services.local.pathfinder.engine.collision.SparseBitSet;
import osrs.dev.tiletypemap.ITileTypeMap;
import osrs.dev.dumper.ICoordPacker;
import osrs.dev.dumper.ConfigurableCoordPacker;
import osrs.dev.tiletypemap.TileType;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

/**
 * SparseBitSet-based tile type map with 4-bit type encoding.
 * Uses bits 28-31 to encode type values 0-15.
 */
public class SparseBitSetTileTypeMap implements ITileTypeMap {

    private final SparseBitSet bitSet;
    /**
     * Turns out this data structure can't hold a index 32 bit value like RoaringBitMap can
     * so the encoding must be even more compact and does not support plane
     */
    private static final ICoordPacker packing = ConfigurableCoordPacker.SPARSE_TILETYPE_MAP_PACKING_NO_PLANE;
    public static final int SPARSE_TILE_TYPE_BIT_0 = 1 << 26;  // value 1
    public static final int SPARSE_TILE_TYPE_BIT_1 = 1 << 27;  // value 2
    public static final int SPARSE_TILE_TYPE_BIT_2 = 1 << 28;  // value 4
    public static final int SPARSE_TILE_TYPE_BIT_3 = 1 << 29;  // value 8

    private SparseBitSetTileTypeMap(SparseBitSet bitSet) {
        this.bitSet = bitSet;
    }

    @Override
    public byte getTileType(int x, int y, int plane) {
        if (plane != 0) return 0; // Only plane 0 is supported in this implementation
        int packed = packing.pack(x, y, plane);
        byte type = 0;
        if (bitSet.get(packed | SPARSE_TILE_TYPE_BIT_0)) type |= 0b0001;
        if (bitSet.get(packed | SPARSE_TILE_TYPE_BIT_1)) type |= 0b0010;
        if (bitSet.get(packed | SPARSE_TILE_TYPE_BIT_2)) type |= 0b0100;
        if (bitSet.get(packed | SPARSE_TILE_TYPE_BIT_3)) type |= 0b1000;
        return type;
    }

    /**
     * Loads from an input stream.
     * The input stream should already be decompressed if it was gzipped.
     *
     * @param inputStream The input stream containing serialized SparseBitSet.
     * @return The tile type map.
     * @throws IOException            On file read error.
     * @throws ClassNotFoundException On class not found.
     */
    public static SparseBitSetTileTypeMap load(InputStream inputStream) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return new SparseBitSetTileTypeMap((SparseBitSet) objectInputStream.readObject());
        }
    }
}
