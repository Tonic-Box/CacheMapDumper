package osrs.dev;

import lombok.extern.slf4j.Slf4j;
import osrs.dev.collisionmap.CollisionMap;
import osrs.dev.collisionmap.CollisionMapFactory;

import java.io.File;

/**
 * Launches the collision map viewer
 */
@Slf4j
public class Benchmark
{
    public static File SPARSE_MAP = new File(System.getProperty("user.home") + "/VitaX/map_sparse.dat.gz");
    public static File ROARING_MAP = new File(System.getProperty("user.home") + "/VitaX/map_roaring.dat.gz");

    private static final int MIN_X = 1500;
    private static final int MAX_X = 3500;
    private static final int MIN_Y = 1500;
    private static final int MAX_Y = 3500;
    private static final int MIN_PLANE = 0;
    private static final int MAX_PLANE = 2;

    /**
     * Generates a random X coordinate between 1500-3500
     */
    private static int randomX(java.util.Random random) {
        return MIN_X + random.nextInt(MAX_X - MIN_X + 1);
    }

    /**
     * Generates a random Y coordinate between 1500-3500
     */
    private static int randomY(java.util.Random random) {
        return MIN_Y + random.nextInt(MAX_Y - MIN_Y + 1);
    }

    /**
     * Generates a random plane between 0-2
     */
    private static int randomPlane(java.util.Random random) {
        return MIN_PLANE + random.nextInt(MAX_PLANE - MIN_PLANE + 1);
    }

    /**
     * Measures current heap memory usage in bytes.
     * Triggers GC before measurement for accuracy.
     */
    private static long measureMemoryUsage() {
        System.gc();
        System.gc();
        System.gc();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Formats bytes into human-readable format (KB, MB, GB)
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java osrs.dev.Benchmark <roaring|sparse> <random|realistic>");
            System.err.println("  Format:");
            System.err.println("    roaring - benchmark RoaringBitmap format");
            System.err.println("    sparse  - benchmark SparseBitSet format");
            System.err.println("  Test mode:");
            System.err.println("    random     - random coordinate reads");
            System.err.println("    realistic - realistic coordinate reads (more cache-friendly)");
            System.exit(1);
        }

        String format = args[0].toLowerCase();
        String testMode = args[1].toLowerCase();
        File mapFile;

        if (format.equals("roaring")) {
            mapFile = ROARING_MAP;
        } else if (format.equals("sparse")) {
            mapFile = SPARSE_MAP;
        } else {
            System.err.println("Error: Invalid format '" + format + "'. Must be 'roaring' or 'sparse'");
            System.exit(1);
            return;
        }

        if (!testMode.equals("random") && !testMode.equals("realistic")) {
            System.err.println("Error: Invalid test mode '" + testMode + "'. Must be 'random' or 'realistic'");
            System.exit(1);
        }

        if (!mapFile.exists()) {
            System.err.println("Error: Map file not found at " + mapFile.getAbsolutePath());
            System.exit(1);
        }

        // Measure baseline memory
        long baselineMemory = measureMemoryUsage();

        System.out.println("Loading map: " + mapFile.getAbsolutePath());
        CollisionMap map = CollisionMapFactory.load(mapFile.getAbsolutePath());
        System.out.println("Map loaded successfully");

        // Measure memory after loading
        long loadedMemory = measureMemoryUsage();
        long mapMemoryUsage = loadedMemory - baselineMemory;
        long fileSize = mapFile.length();

        // Display memory usage
        System.out.println("\nMemory Usage:");
        System.out.println("=============");
        System.out.println("Baseline: " + formatBytes(baselineMemory));
        System.out.println("After load: " + formatBytes(loadedMemory));
        System.out.println("Map size: " + formatBytes(mapMemoryUsage));
        System.out.println("File size on disk: " + formatBytes(fileSize));
        if (mapMemoryUsage > 0) {
            System.out.println("Memory/Disk ratio: " + String.format("%.2fx", mapMemoryUsage / (double) fileSize));
        }

        System.out.println("\nTest mode: " + testMode);

        if (testMode.equalsIgnoreCase("realistic")) {
            benchmarkRealistic(map);
        } else if (testMode.equalsIgnoreCase("random")){
            benchmarkRandom(map);
        } else {
            System.err.println("Error: Unknown test mode '" + testMode + "'");
            System.exit(1);
        }
    }

    /**
     * Benchmark with random coordinate reads
     */
    private static void benchmarkRandom(CollisionMap map) {
        java.util.Random random = new java.util.Random(42);

        // Warmup phase
        System.out.println("\nWarming up JIT (100k reads)...");
        long totalWalkable = 0;
        for (int i = 0; i < 100_000; i++) {
            int x = randomX(random);
            int y = randomY(random);
            int plane = randomPlane(random);

            switch (i % 4) {
                case 0: totalWalkable += map.pathableNorth(x, y, plane) ? 1 : 0; break;
                case 1: totalWalkable += map.pathableEast(x, y, plane) ? 1 : 0; break;
                case 2: totalWalkable += map.pathableSouth(x, y, plane) ? 1 : 0; break;
                case 3: totalWalkable += map.pathableWest(x, y, plane) ? 1 : 0; break;
            }
        }
        System.out.println("Warmup complete");

        // Benchmark phase
        System.out.println("\nRunning benchmark (100,000,000 random reads)...");
        random.setSeed(42);
        totalWalkable = 0;

        long startTime = System.nanoTime();

        for (int i = 0; i < 100_000_000; i++) {
            int x = randomX(random);
            int y = randomY(random);
            int plane = randomPlane(random);

            switch (i % 4) {
                case 0: totalWalkable += map.pathableNorth(x, y, plane) ? 1 : 0; break;
                case 1: totalWalkable += map.pathableEast(x, y, plane) ? 1 : 0; break;
                case 2: totalWalkable += map.pathableSouth(x, y, plane) ? 1 : 0; break;
                case 3: totalWalkable += map.pathableWest(x, y, plane) ? 1 : 0; break;
            }
        }

        long endTime = System.nanoTime();
        printResults(endTime - startTime, 100_000_000, totalWalkable);
    }

    /**
     * Benchmark with realistic coordinate reads
     */
    private static void benchmarkRealistic(CollisionMap map) {
        // Warmup phase
        System.out.println("\nWarming up JIT...");
        long warmupReads = 0;
        long totalWalkable = 0;
        for (int x = MIN_X; x <= Math.min(MIN_X + 10, MAX_X); x++) {
            for (int y = MIN_Y; y <= Math.min(MIN_Y + 10, MAX_Y); y++) {
                for (int plane = MIN_PLANE; plane <= MAX_PLANE; plane++) {
                    totalWalkable += map.pathableNorth(x, y, plane) ? 1 : 0;
                    totalWalkable += map.pathableEast(x, y, plane) ? 1 : 0;
                    totalWalkable += map.pathableSouth(x, y, plane) ? 1 : 0;
                    totalWalkable += map.pathableWest(x, y, plane) ? 1 : 0;
                    warmupReads += 4;
                }
            }
        }
        System.out.println("Warmup complete (" + warmupReads + " reads)");

        // Benchmark phase
        System.out.println("\nRunning benchmark (sequential reads)...");

        long startTime = System.nanoTime();
        long readCount = 0;
        totalWalkable = 0;

        for (int repeats = 0; repeats < 20; repeats++) {
            for (int x = MIN_X; x <= MAX_X; x++) {
                for (int y = MIN_Y; y <= MAX_Y; y++) {
                    for (int plane = MIN_PLANE; plane <= MAX_PLANE; plane++) {
                        totalWalkable += map.pathableNorth(x, y, plane) ? 1 : 0;
                        totalWalkable += map.pathableEast(x, y, plane) ? 1 : 0;
                        totalWalkable += map.pathableSouth(x, y, plane) ? 1 : 0;
                        totalWalkable += map.pathableWest(x, y, plane) ? 1 : 0;
                        readCount += 4;
                    }
                }
            }
        }

        long endTime = System.nanoTime();
        printResults(endTime - startTime, readCount, totalWalkable);
    }

    /**
     * Print benchmark results
     */
    private static void printResults(long durationNanos, long readCount, long totalWalkable) {
        double durationMillis = durationNanos / 1_000_000.0;
        double durationSeconds = durationNanos / 1_000_000_000.0;
        double readsPerSecond = readCount / durationSeconds;

        System.out.println("\nBenchmark Results:");
        System.out.println("==================");
        System.out.println("Total reads: " + String.format("%,d", readCount));
        System.out.println("Total time: " + String.format("%.2f ms", durationMillis));
        System.out.println("Total time: " + String.format("%.4f seconds", durationSeconds));
        System.out.println("Average per read: " + String.format("%.2f ns", durationNanos / (double) readCount));
        System.out.println("Reads per second: " + String.format("%.0f", readsPerSecond));
        System.out.println("Walkable count: " + String.format("%,d", totalWalkable));
    }
}
