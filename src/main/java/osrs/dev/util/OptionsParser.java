package osrs.dev.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import osrs.dev.collision.CollisionMapFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses command line options.
 */
@Getter
@Slf4j
public class OptionsParser
{
    private String path = System.getProperty("user.home") + "/VitaX/collision/map.dat";
    private boolean freshCache = true;
    private CollisionMapFactory.Format format = CollisionMapFactory.Format.ROARING_GZIP;

    public OptionsParser(String[] args) {
        for(int i = 0; i < args.length; ++i) {
            switch (args[i]) {
                case "-path":
                    path = args[++i];
                    break;
                case "-fresh":
                    freshCache = args[++i].toLowerCase().startsWith("y");
                    break;
                case "-format":
                    String formatStr = args[++i];
                    if ("SparseBitSet".equalsIgnoreCase(formatStr)) {
                        format = CollisionMapFactory.Format.SPARSE_BITSET;
                    } else if ("RoaringBitmap".equalsIgnoreCase(formatStr)) {
                        format = CollisionMapFactory.Format.ROARING_GZIP;
                    } else {
                        log.warn("Unknown format: {}, defaulting to RoaringBitmap", formatStr);
                    }
                    break;
            }
        }
    }
}
