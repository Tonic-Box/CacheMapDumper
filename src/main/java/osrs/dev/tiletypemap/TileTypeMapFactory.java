package osrs.dev.tiletypemap;

import lombok.extern.slf4j.Slf4j;
import osrs.dev.tiletypemap.roaring.RoaringTileTypeMapWriter;
import osrs.dev.tiletypemap.sparse.SparseBitSetTileTypeMapWriter;
import osrs.dev.tiletypemap.roaring.RoaringTileTypeMap;
import osrs.dev.tiletypemap.sparse.SparseBitSetTileTypeMap;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Factory for loading tile type maps and creating writers.
 * Auto-detects file format from filename ("roaring" or "sparse") and gzip from extension (.gz).
 */
@Slf4j
public class TileTypeMapFactory {

    /**
     * Supported tile type map formats.
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
         */
        ROARING
    }

    private TileTypeMapFactory() {}

    /**
     * Loads a tile type map, auto-detecting the format and handling gzip decompression.
     *
     * @param filePath path to the tile type map file
     * @return the loaded tile type map
     * @throws Exception if loading fails
     */
    public static ITileTypeMap load(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("File not found: " + filePath);
            return null;
        }

        Format format = detectFormat(filePath);
        boolean gzipped = isGzipped(filePath);
        log.debug("Loading tile type map in format: {}, gzipped: {}", format, gzipped);

        try (FileInputStream fis = new FileInputStream(file);
             InputStream inputStream = gzipped ? new GZIPInputStream(fis) : fis) {

            switch (format) {
                case ROARING:
                    return RoaringTileTypeMap.load(inputStream);
                case SPARSE_BITSET:
                default:
                    return SparseBitSetTileTypeMap.load(inputStream);
            }
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
     * @return a new tile type map writer
     */
    public static ITileTypeMapWriter createWriter(Format format) {
        switch (format) {
            case ROARING:
                return new RoaringTileTypeMapWriter();
            case SPARSE_BITSET:
            default:
                return new SparseBitSetTileTypeMapWriter();
        }
    }

    /**
     * Creates a new writer for the default format (RoaringBitmap).
     *
     * @return a new tile type map writer
     */
    public static ITileTypeMapWriter createWriter() {
        return createWriter(Format.ROARING);
    }
}
