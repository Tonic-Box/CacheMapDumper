package osrs.dev.collisionmap.sparse;

import VitaX.services.local.pathfinder.engine.collision.SparseBitSet;
import osrs.dev.collisionmap.ICollisionMap;
import osrs.dev.dumper.ICoordIndexer;
import osrs.dev.dumper.ConfigurableCoordIndexer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class SparseBitSetCollisionMap implements ICollisionMap {
    static final ICoordIndexer INDEXER = ConfigurableCoordIndexer.SPARSEBITSET_3BIT_DATA_COORD_INDEXER;
    static final int NORTH_DATA_BIT_POS = 0;
    static final int EAST_DATA_BIT_POS = 1;
    private final SparseBitSet bitSet;

    /**
     * Creates a new collision map with default legacy packing.
     *
     * @param bitSet The bit set.
     */
    private SparseBitSetCollisionMap(SparseBitSet bitSet) {
        this.bitSet = bitSet;
    }

    // ==================== ICollisionMap interface ====================

    @Override
    public boolean pathableNorth(int x, int y, int plane) {
        return !bitSet.get(INDEXER.packToBitmapIndex(x, y, plane, NORTH_DATA_BIT_POS));
    }

    @Override
    public boolean pathableEast(int x, int y, int plane) {
        return !bitSet.get(INDEXER.packToBitmapIndex(x, y, plane, EAST_DATA_BIT_POS));
    }

    /**
     * Loads a collision map from an input stream.
     * The input stream should already be decompressed if it was gzipped.
     *
     * @param inputStream The input stream containing serialized SparseBitSet.
     * @return The collision map.
     * @throws IOException            On file read error.
     * @throws ClassNotFoundException On class not found.
     */
    public static SparseBitSetCollisionMap load(InputStream inputStream) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return new SparseBitSetCollisionMap((SparseBitSet) objectInputStream.readObject());
        }
    }
}
