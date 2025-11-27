package osrs.dev.tiletypemap;

import osrs.dev.dumper.ICoordIndexer;
import osrs.dev.tiledatamap.ITileDataMap;

/**
 * Generic tile type map backed by any ITileDataMap implementation.
 * Provides tile type semantics over generic bit storage.
 */
public class TileTypeMap {
    private final ITileDataMap dataMap;

    public TileTypeMap(ITileDataMap dataMap) {
        this.dataMap = dataMap;
    }

    public ICoordIndexer getIndexer() {
        return dataMap.getIndexer();
    }

    public boolean isDataBitSet(int x, int y, int plane, int dataBitIndex) {
        return dataMap.isDataBitSet(x, y, plane, dataBitIndex);
    }

    public byte getTileType(int x, int y, int plane) {
        int data = 0;
        for (int i = 0; i <= getIndexer().getMaxDataBitIndex(); i++) {
            if (isDataBitSet(x, y, plane, i)) {
                data |= (1 << i);
            }
        }
        return (byte) data;
    }
}
