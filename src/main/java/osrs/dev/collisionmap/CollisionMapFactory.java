package osrs.dev.collisionmap;

import lombok.extern.slf4j.Slf4j;
import osrs.dev.tiledatamap.ITileDataMap;
import osrs.dev.tiledatamap.ITileDataMapWriter;
import osrs.dev.tiledatamap.roaring.RoaringTileDataMap;
import osrs.dev.tiledatamap.roaring.RoaringTileDataMapWriter;
import osrs.dev.tiledatamap.sparse.SparseTileDataMap;
import osrs.dev.tiledatamap.sparse.SparseTileDataMapWriter;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Factory for loading collision maps and creating writers.
 * Auto-detects file format from filename ("roaring" or "sparse") and gzip from extension (.gz).
 */
@Slf4j
public class CollisionMapFactory {

    /**
     * Supported collision map formats.
     */
    public enum Format {
        /**
         * Legacy Java ObjectOutputStream format with SparseBitSet.
         * Detected by "sparse" in the filename.
         */
        SPARSE_BITSET,

        /**
         * RoaringBitmap format.
         * Detected by "roaring" in the filename.
         * Can be gzipped (check with isGzipped()).
         */
        ROARING
    }

    private CollisionMapFactory() {}

    /**
     * Loads a collision map, auto-detecting the format and handling gzip decompression.
     *
     * @param filePath path to the collision map file
     * @return the loaded collision map
     * @throws Exception if loading fails
     */
    public static CollisionMap load(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("File not found: " + filePath);
            return null;
        }

        Format format = detectFormat(filePath);
        boolean gzipped = isGzipped(filePath);
        log.debug("Loading map in format: {}, gzipped: {}", format, gzipped);

        try (FileInputStream fis = new FileInputStream(file);
             InputStream inputStream = gzipped ? new GZIPInputStream(fis) : fis) {

            ITileDataMap dataMap;
            switch (format) {
                case ROARING:
                    dataMap = RoaringTileDataMap.load(inputStream);
                    break;
                case SPARSE_BITSET:
                default:
                    dataMap = SparseTileDataMap.load(inputStream);
                    break;
            }
            return new CollisionMap(dataMap);
        }
    }

    /**
     * Detects file format by examining the filename.
     * Looks for "roaring" or "sparse" in the path.
     *
     * @param filePath path to the file
     * @return detected format, defaults to ROARING for unknown formats
     */
    public static Format detectFormat(String filePath) {
        String lowerPath = filePath.toLowerCase();

        if (lowerPath.contains("roaring")) {
            return Format.ROARING;
        } else if (lowerPath.contains("sparse")) {
            return Format.SPARSE_BITSET;
        }

        // Default to ROARING for new files
        return Format.ROARING;
    }

    /**
     * Determines if a file is gzipped by checking the extension.
     *
     * @param filePath path to the file
     * @return true if the file ends with .gz, false otherwise
     */
    public static boolean isGzipped(String filePath) {
        return filePath.endsWith(".gz");
    }

    /**
     * Creates a new writer for the specified format.
     *
     * @param format the format to write
     * @return a new collision map writer
     */
    public static CollisionMapWriter createWriter(Format format) {
        ITileDataMapWriter dataMapWriter;
        switch (format) {
            case ROARING:
                dataMapWriter = new RoaringTileDataMapWriter();
                break;
            case SPARSE_BITSET:
            default:
                dataMapWriter = new SparseTileDataMapWriter();
                break;
        }
        return new CollisionMapWriter(dataMapWriter);
    }
}
