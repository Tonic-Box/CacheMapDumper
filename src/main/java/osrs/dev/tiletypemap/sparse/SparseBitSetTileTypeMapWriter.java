package osrs.dev.tiletypemap.sparse;

import VitaX.services.local.pathfinder.engine.collision.SparseBitSet;
import osrs.dev.tiletypemap.ITileTypeMapWriter;
import osrs.dev.dumper.ICoordPacker;
import osrs.dev.dumper.ConfigurableCoordPacker;
import osrs.dev.tiletypemap.TileType;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * SparseBitSet-based tile type map writer with 4-bit type encoding.
 * Uses bits 28-31 to encode type values 0-15.
 */
public class SparseBitSetTileTypeMapWriter implements ITileTypeMapWriter {

    private final SparseBitSet bitSet;
    private static final ICoordPacker packing = ConfigurableCoordPacker.COMPACT_13BIT_PACKING;

    public SparseBitSetTileTypeMapWriter() {
        this.bitSet = new SparseBitSet();
    }

    @Override
    public void setTileType(int x, int y, int plane, int type) {
        int packed = packing.pack(x, y, plane);
        if ((type & 1) != 0) bitSet.set(packed | TileType.TILE_TYPE_BIT_0);
        if ((type & 2) != 0) bitSet.set(packed | TileType.TILE_TYPE_BIT_1);
        if ((type & 4) != 0) bitSet.set(packed | TileType.TILE_TYPE_BIT_2);
        if ((type & 8) != 0) bitSet.set(packed | TileType.TILE_TYPE_BIT_3);
    }

    @Override
    public void save(String filePath) throws IOException {
        if (filePath.endsWith(".gz")) {
            saveGzipped(filePath);
        } else {
            saveWithoutGzip(filePath);
        }
    }

    /**
     * Saves the tile type map with GZIP compression.
     *
     * @param filePath path to save the tile type map
     * @throws IOException if saving fails
     */
    public void saveGzipped(String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(gzos)) {
            oos.writeObject(bitSet);
        }
    }

    /**
     * Saves the tile type map without GZIP compression.
     *
     * @param filePath path to save the tile type map
     * @throws IOException if saving fails
     */
    public void saveWithoutGzip(String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(bitSet);
        }
    }
}
