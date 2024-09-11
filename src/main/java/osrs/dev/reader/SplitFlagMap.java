package osrs.dev.reader;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPInputStream;

public abstract class SplitFlagMap {
    private static final int MAXIMUM_SIZE = 20 * 1024 * 1024;
    private final int regionSize;
    private final LoadingCache<Position, FlagMap> regionMaps;
    private final int flagCount;

    public SplitFlagMap(int regionSize, Map<Position, byte[]> compressedRegions, int flagCount) {
        this.regionSize = regionSize;
        this.flagCount = flagCount;
        regionMaps = CacheBuilder
                .newBuilder()
                .weigher((Weigher<Position, FlagMap>) (k, v) -> v.flags.size() / 8)
                .maximumWeight(MAXIMUM_SIZE)
                .build(CacheLoader.from(position -> {
                    byte[] compressedRegion = compressedRegions.get(position);

                    if (compressedRegion == null) {
                        return new FlagMap(position.x * regionSize, position.y * regionSize, (position.x + 1) * regionSize - 1, (position.y + 1) * regionSize - 1, this.flagCount);
                    }

                    try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(compressedRegion))) {
                        return new FlagMap(readAllBytes(in), this.flagCount);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }));
    }

    public boolean get(int x, int y, int z, int flag) {
        try {
            return regionMaps.get(new Position(x / regionSize, y / regionSize)).get(x, y, z, flag);
        } catch (ExecutionException e) {
            throw new UncheckedExecutionException(e);
        }
    }

    public static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        while (true) {
            int read = in.read(buffer, 0, buffer.length);

            if (read == -1) {
                return result.toByteArray();
            }

            result.write(buffer, 0, read);
        }
    }

    public static class Position {
        public final int x;
        public final int y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Position &&
                    ((Position) o).x == x &&
                    ((Position) o).y == y;
        }

        @Override
        public int hashCode() {
            return x * 31 + y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
}