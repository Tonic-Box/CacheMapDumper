package osrs.dev.ArbitraryDataBitmap;


import org.roaringbitmap.RoaringBitmap;
import osrs.dev.dumper.ConfigurableCoordPacker;
import osrs.dev.dumper.ICoordPacker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * RoaringBitmap-based tile type map with 4-bit type encoding.
 * Uses bits 28-32 to store arbitrary data.
 */
public class ArbitraryDataBitmap {
    public static final int DATA_BIT_0 = 1 << 28;
    public static final int DATA_BIT_1 = 1 << 29;
    public static final int DATA_BIT_2 = 1 << 30;
    public static final int DATA_BIT_3 = 1 << 31;
    public static final ICoordPacker packing = ConfigurableCoordPacker.COMPACT_13BIT_PACKING;

    private final RoaringBitmap bitmap;

    public ArbitraryDataBitmap(RoaringBitmap bitmap) {
        this.bitmap = bitmap;
    }

    public byte getData(int x, int y, int plane) {
        int packed = packing.pack(x, y, plane);
        byte type = 0;
        if (bitmap.contains(packed | DATA_BIT_0)) type |= 0b00001;
        if (bitmap.contains(packed | DATA_BIT_1)) type |= 0b00010;
        if (bitmap.contains(packed | DATA_BIT_2)) type |= 0b00100;
        if (bitmap.contains(packed | DATA_BIT_3)) type |= 0b01000;
        return type;
    }

    /**
     * Loads from RoaringBitmap native format.
     * The input stream should already be decompressed if it was gzipped.
     *
     * @param inputStream input stream containing RoaringBitmap data
     * @return loaded tile type map
     * @throws IOException if an I/O error occurs
     */
    public static ArbitraryDataBitmap load(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        RoaringBitmap bitmap = new RoaringBitmap();
        bitmap.deserialize(ByteBuffer.wrap(bytes));
        bitmap.runOptimize();
        return new ArbitraryDataBitmap(bitmap);
    }
}
