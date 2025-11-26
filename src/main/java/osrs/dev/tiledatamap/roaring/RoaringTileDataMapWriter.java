package osrs.dev.tiledatamap.roaring;

import org.roaringbitmap.RoaringBitmap;
import osrs.dev.dumper.ConfigurableCoordIndexer;
import osrs.dev.tiledatamap.ITileDataMapWriter;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * Generic RoaringBitmap-based data map writer.
 * Writes arbitrary data bits at tile coordinates using RoaringBitmap.
 */
public class RoaringTileDataMapWriter implements ITileDataMapWriter {
    static final ConfigurableCoordIndexer INDEXER
            = RoaringTileDataMap.INDEXER.withValidationEnabled();
    private final RoaringBitmap bitmap;

    public RoaringTileDataMapWriter() {
        this.bitmap = new RoaringBitmap();
    }

    @Override
    public synchronized void setDataBit(int x, int y, int plane, int dataBitIndex) {
        bitmap.add(INDEXER.packToBitmapIndex(x, y, plane, dataBitIndex));
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
     * Saves the data map with GZIP compression.
     *
     * @param filePath path to save the data map
     * @throws IOException if saving fails
     */
    public void saveGzipped(String filePath) throws IOException {
        bitmap.runOptimize();

        try (FileOutputStream fos = new FileOutputStream(filePath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(gzos)) {

            bitmap.serialize(dos);
        }
    }

    /**
     * Saves the data map without GZIP compression.
     *
     * @param filePath path to save the data map
     * @throws IOException if saving fails
     */
    public void saveWithoutGzip(String filePath) throws IOException {
        bitmap.runOptimize();

        try (FileOutputStream fos = new FileOutputStream(filePath);
             DataOutputStream dos = new DataOutputStream(fos)) {

            bitmap.serialize(dos);
        }
    }
}
