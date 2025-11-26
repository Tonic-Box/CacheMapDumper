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
    private static final ICoordPacker packing = ConfigurableCoordPacker.COMPACT_13BIT_PACKING;

    private SparseBitSetTileTypeMap(SparseBitSet bitSet) {
        this.bitSet = bitSet;
    }

    @Override
    public byte getTileType(int x, int y, int plane) {
        int packed = packing.pack(x, y, plane);
        byte type = 0;
        if (bitSet.get(packed | TileType.TILE_TYPE_BIT_0)) type |= 1;
        if (bitSet.get(packed | TileType.TILE_TYPE_BIT_1)) type |= 2;
        if (bitSet.get(packed | TileType.TILE_TYPE_BIT_2)) type |= 4;
        if (bitSet.get(packed | TileType.TILE_TYPE_BIT_3)) type |= 8;
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
