package osrs.dev.tiletypemap.roaring;

import org.roaringbitmap.RoaringBitmap;
import osrs.dev.dumper.ConfigurableCoordIndexer;
import osrs.dev.dumper.ICoordIndexer;
import osrs.dev.tiletypemap.ITileTypeMap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class RoaringTileTypeMap implements ITileTypeMap {
    static final ConfigurableCoordIndexer INDEXER
            = ConfigurableCoordIndexer.ROARINGBITMAP_4BIT_DATA_COORD_INDEXER;

    private final RoaringBitmap bitmap;

    public RoaringTileTypeMap(RoaringBitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    public ICoordIndexer getIndexer() {
        return INDEXER;
    }

    public boolean isDataBitSet(int x, int y, int plane, int dataBitIndex) {
        int bitIndex = INDEXER.packToBitmapIndex(x, y, plane, dataBitIndex);
        return bitmap.contains(bitIndex);
    }

    /**
     * Loads from RoaringBitmap native format.
     * The input stream should already be decompressed if it was gzipped.
     *
     * @param inputStream input stream containing RoaringBitmap data
     * @return loaded tile type map
     * @throws IOException if an I/O error occurs
     */
    public static RoaringTileTypeMap load(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        RoaringBitmap bitmap = new RoaringBitmap();
        bitmap.deserialize(ByteBuffer.wrap(bytes));
        bitmap.runOptimize();
        return new RoaringTileTypeMap(bitmap);
    }
}
