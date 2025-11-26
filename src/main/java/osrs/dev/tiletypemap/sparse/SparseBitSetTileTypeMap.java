package osrs.dev.tiletypemap.sparse;

import VitaX.services.local.pathfinder.engine.collision.SparseBitSet;
import osrs.dev.dumper.ConfigurableCoordIndexer;
import osrs.dev.dumper.ICoordIndexer;
import osrs.dev.tiletypemap.ITileTypeMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class SparseBitSetTileTypeMap implements ITileTypeMap {
     static final ConfigurableCoordIndexer INDEXER
             = ConfigurableCoordIndexer.SPARSEBITSET_4BIT_DATA_COORD_INDEXER;

    private final SparseBitSet bitSet;

    private SparseBitSetTileTypeMap(SparseBitSet bitSet) {
        this.bitSet = bitSet;
    }

    @Override
    public ICoordIndexer getIndexer() {
        return INDEXER;
    }

    public boolean isDataBitSet(int x, int y, int plane, int dataBitIndex) {
        int bitIndex = INDEXER.packToBitmapIndex(x, y, plane, dataBitIndex);
        return bitSet.get(bitIndex);
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
