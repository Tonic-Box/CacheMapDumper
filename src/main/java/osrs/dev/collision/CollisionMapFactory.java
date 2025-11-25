package osrs.dev.collision;

import lombok.extern.slf4j.Slf4j;
import osrs.dev.dumper.SparseBitsetMapWriter;
import osrs.dev.reader.SparseBitSetCollisionMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Factory for loading collision maps and creating writers.
 * Auto-detects file format based on magic bytes.
 */
@Slf4j
public class CollisionMapFactory {

    /**
     * Supported collision map formats.
     */
    public enum Format {
        /**
         * Legacy Java ObjectOutputStream format with SparseBitSet.
         * Magic bytes: 0xac 0xed (Java serialization)
         */
        SPARSE_BITSET,

        /**
         * RoaringBitmap with GZIP compression.
         * Magic bytes: 0x1f 0x8b (GZIP)
         */
        ROARING_GZIP
    }

    private CollisionMapFactory() {}

    /**
     * Loads a collision map, auto-detecting the format.
     *
     * @param filePath path to the collision map file
     * @return the loaded collision map
     * @throws Exception if loading fails
     */
    public static ICollisionMap load(String filePath) throws Exception {
        Format format = detectFormat(filePath);
        log.debug("Loading map in format: {}", format);

        switch (format) {
            case ROARING_GZIP:
                return RoaringCollisionMap.load(filePath);
            case SPARSE_BITSET:
            default:
                return SparseBitSetCollisionMap.load(filePath);
        }
    }

    /**
     * Detects file format by examining magic bytes.
     *
     * @param filePath path to the file
     * @return detected format, defaults to SPARSE_BITSET for unknown formats
     * @throws IOException if file cannot be read
     */
    public static Format detectFormat(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return Format.ROARING_GZIP; // Default for new files (new default format)
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[2];
            if (fis.read(header) < 2) {
                return Format.SPARSE_BITSET;
            }

            // GZIP magic number: 0x1f 0x8b
            if (header[0] == (byte) 0x1f && header[1] == (byte) 0x8b) {
                return Format.ROARING_GZIP;
            }

            // Java serialization magic: 0xac 0xed
            if (header[0] == (byte) 0xac && header[1] == (byte) 0xed) {
                return Format.SPARSE_BITSET;
            }
        }

        return Format.SPARSE_BITSET;
    }

    /**
     * Creates a new writer for the specified format.
     *
     * @param format the format to write
     * @return a new collision map writer
     */
    public static ICollisionMapWriter createWriter(Format format) {
        switch (format) {
            case ROARING_GZIP:
                return new RoaringCollisionMapWriter();
            case SPARSE_BITSET:
            default:
                return new SparseBitsetMapWriter();
        }
    }

    /**
     * Creates a new writer for the default format (RoaringBitmap GZIP).
     *
     * @return a new collision map writer
     */
    public static ICollisionMapWriter createWriter() {
        return createWriter(Format.ROARING_GZIP);
    }
}
