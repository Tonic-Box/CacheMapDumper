package osrs.dev.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import osrs.dev.collisionmap.CollisionMapFactory;

/**
 * Parses command line options.
 */
@Getter
@Slf4j
public class OptionsParser
{
    private String outputDir = System.getProperty("user.home") + "/VitaX/";
    private boolean freshCache = true;
    private CollisionMapFactory.Format format = CollisionMapFactory.Format.ROARING;

    public OptionsParser(String[] args) {
        for(int i = 0; i < args.length; ++i) {
            switch (args[i]) {
                case "-dir":
                    outputDir = args[++i];
                    break;
                case "-fresh":
                    freshCache = args[++i].toLowerCase().startsWith("y");
                    break;
                case "-format":
                    String formatStr = args[++i];
                    if ("SparseBitSet".equalsIgnoreCase(formatStr)) {
                        format = CollisionMapFactory.Format.SPARSE_BITSET;
                    } else if ("RoaringBitmap".equalsIgnoreCase(formatStr)) {
                        format = CollisionMapFactory.Format.ROARING;
                    } else {
                        log.warn("Unknown format: {}, defaulting to RoaringBitmap", formatStr);
                    }
                    break;
            }
        }
    }

    /**
     * Gets the collision map file path based on output directory and format.
     */
    public String getCollisionMapPath() {
        String dir = outputDir;
        if (!dir.endsWith("/") && !dir.endsWith("\\")) {
            dir += "/";
        }
        if (format == CollisionMapFactory.Format.SPARSE_BITSET) {
            return dir + "map_sparse.dat.gz";
        } else {
            return dir + "map_roaring.dat.gz";
        }
    }

    /**
     * Gets the tile type map file path based on output directory and format.
     */
    public String getTileTypeMapPath() {
        String dir = outputDir;
        if (!dir.endsWith("/") && !dir.endsWith("\\")) {
            dir += "/";
        }
        if (format == CollisionMapFactory.Format.SPARSE_BITSET) {
            return dir + "tile_types_sparse.dat.gz";
        } else {
            return dir + "tile_types_roaring.dat.gz";
        }
    }
}
