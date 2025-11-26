package osrs.dev.tiletypemap;

import osrs.dev.tiledatamap.ITileDataMapWriter;

import java.io.IOException;

/**
 * Generic tile type map writer backed by any ITileDataMapWriter implementation.
 * Provides tile type semantics over generic bit storage.
 */
public class TileTypeMapWriter  {
    private final ITileDataMapWriter dataMapWriter;

    public TileTypeMapWriter(ITileDataMapWriter dataMapWriter) {
        this.dataMapWriter = dataMapWriter;
    }

    public void setDataBit(int x, int y, int plane, int dataBitIndex) {
        dataMapWriter.setDataBit(x, y, plane, dataBitIndex);
    }

    public void setTileType(int x, int y, int plane, byte type){
        if ((type & 0b0001) != 0) setDataBit(x, y, plane, 0);
        if ((type & 0b0010) != 0) setDataBit(x, y, plane, 1);
        if ((type & 0b0100) != 0) setDataBit(x, y, plane, 2);
        if ((type & 0b1000) != 0) setDataBit(x, y, plane, 3);
    }

    public void save(String filePath) throws IOException {
        dataMapWriter.save(filePath);
    }
}
