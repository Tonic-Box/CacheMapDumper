package osrs.dev.reader;

import java.io.*;
import java.util.*;

/**
 * Optimized object ID reader using sorted arrays and binary search.
 * Much faster than HashMap for sparse coordinate data.
 */
public class ObjectMapOptimized {
    private final long[] coordinates;  // Sorted array of packed coordinates
    private final int[] offsets;       // Offset into objectIds array for each coordinate
    private final int[] objectIds;     // Flattened array of all object IDs
    private final int version;

    private ObjectMapOptimized(long[] coordinates, int[] offsets, int[] objectIds, int version) {
        this.coordinates = coordinates;
        this.offsets = offsets;
        this.objectIds = objectIds;
        this.version = version;
    }

    /**
     * Gets all object IDs at the specified world coordinates.
     * Uses binary search for O(log n) lookup time.
     */
    public List<Integer> getObjects(short x, short y, byte z) {
        long packedCoord = packCoordinate(x, y, z);

        // Binary search for coordinate
        int index = Arrays.binarySearch(coordinates, packedCoord);

        if (index < 0) {
            return Collections.emptyList();
        }

        // Get start and end offsets for this coordinate's objects
        int startOffset = offsets[index];
        int endOffset = (index + 1 < offsets.length) ? offsets[index + 1] : objectIds.length;

        // Build result list
        List<Integer> result = new ArrayList<>(endOffset - startOffset);
        for (int i = startOffset; i < endOffset; i++) {
            result.add(objectIds[i]);
        }

        return result;
    }

    /**
     * Checks if there are any objects at the specified coordinates.
     */
    public boolean hasObjects(short x, short y, byte z) {
        long packedCoord = packCoordinate(x, y, z);
        return Arrays.binarySearch(coordinates, packedCoord) >= 0;
    }

    /**
     * Gets the number of unique coordinates with objects.
     */
    public int size() {
        return coordinates.length;
    }

    public int getVersion() {
        return version;
    }

    private long packCoordinate(short x, short y, byte z) {
        return (x & 8191) | ((long)(y & 32767) << 13) | ((long)(z & 15) << 28);
    }

    /**
     * Loads an optimized object map from an InputStream (e.g., resource).
     */
    public static ObjectMapOptimized load(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            System.err.println("Object map input stream is null");
            return null;
        }

        long startTime = System.currentTimeMillis();
        int version;
        long[] coordinates;
        int[] offsets;
        int[] objectIds;

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(inputStream, 131072))) {

            version = dis.readInt();
            int entryCount = dis.readInt();

            // Validation: sanity check on entry count
            if (entryCount < 0 || entryCount > 10_000_000) {
                throw new IOException("Invalid entry count: " + entryCount + " (file may be corrupted)");
            }

            System.out.println("Loading object map with " + entryCount + " coordinates...");

            coordinates = new long[entryCount];
            offsets = new int[entryCount];
            List<Integer> objectIdList = new ArrayList<>(entryCount * 3);

            int currentOffset = 0;
            for (int i = 0; i < entryCount; i++) {
                long packedCoord = dis.readLong();
                int objectCount = dis.readInt();

                // Validation: sanity check on object count per coordinate
                if (objectCount < 0 || objectCount > 1000) {
                    throw new IOException("Invalid object count at entry " + i + ": " + objectCount);
                }

                coordinates[i] = packedCoord;
                offsets[i] = currentOffset;

                for (int j = 0; j < objectCount; j++) {
                    objectIdList.add(dis.readInt());
                }

                currentOffset += objectCount;

                // Progress logging for large files
                if (i > 0 && i % 100000 == 0) {
                    System.out.println("Loaded " + i + " / " + entryCount + " coordinates...");
                }
            }

            // Convert to primitive array
            objectIds = new int[objectIdList.size()];
            for (int i = 0; i < objectIdList.size(); i++) {
                objectIds[i] = objectIdList.get(i);
            }
        }

        long loadTime = System.currentTimeMillis() - startTime;
        System.out.println("Loaded optimized object map: " + coordinates.length +
            " coordinates, " + objectIds.length + " objects in " + loadTime + "ms");

        return new ObjectMapOptimized(coordinates, offsets, objectIds, version);
    }

    /**
     * Loads an optimized object map from file.
     */
    public static ObjectMapOptimized load(String filePath) throws IOException {
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            System.err.println("Object map file not found: " + filePath);
            return null;
        }

        long startTime = System.currentTimeMillis();
        int version;
        long[] coordinates;
        int[] offsets;
        int[] objectIds;

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file), 131072))) {

            version = dis.readInt();
            int entryCount = dis.readInt();

            // Validation: sanity check on entry count
            if (entryCount < 0 || entryCount > 10_000_000) {
                throw new IOException("Invalid entry count: " + entryCount + " (file may be corrupted) " + filePath);
            }

            coordinates = new long[entryCount];
            offsets = new int[entryCount];
            List<Integer> objectIdList = new ArrayList<>(entryCount * 3); // Conservative estimate

            int currentOffset = 0;
            for (int i = 0; i < entryCount; i++) {
                long packedCoord = dis.readLong();
                int objectCount = dis.readInt();

                // Validation: sanity check on object count per coordinate
                if (objectCount < 0 || objectCount > 1000) {
                    throw new IOException("Invalid object count at entry " + i + ": " + objectCount);
                }

                coordinates[i] = packedCoord;
                offsets[i] = currentOffset;

                for (int j = 0; j < objectCount; j++) {
                    objectIdList.add(dis.readInt());
                }

                currentOffset += objectCount;
            }

            // Convert to primitive array
            objectIds = new int[objectIdList.size()];
            for (int i = 0; i < objectIdList.size(); i++) {
                objectIds[i] = objectIdList.get(i);
            }
        }

        long loadTime = System.currentTimeMillis() - startTime;
        System.out.println("Loaded optimized object map: " + coordinates.length +
            " coordinates, " + objectIds.length + " objects in " + loadTime + "ms");

        return new ObjectMapOptimized(coordinates, offsets, objectIds, version);
    }
}
