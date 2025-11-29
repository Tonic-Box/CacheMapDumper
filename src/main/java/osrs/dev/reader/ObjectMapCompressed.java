package osrs.dev.reader;

import osrs.dev.util.VarInt;

import java.io.*;
import java.util.*;

/**
 * Compressed object map reader for Version 2 format.
 * Handles delta-encoded coordinates and VarInt encoding.
 */
public class ObjectMapCompressed {
    private final long[] coordinates;
    private final int[] offsets;
    private final int[] objectIds;
    private final int version;

    private ObjectMapCompressed(long[] coordinates, int[] offsets, int[] objectIds, int version) {
        this.coordinates = coordinates;
        this.offsets = offsets;
        this.objectIds = objectIds;
        this.version = version;
    }

    /**
     * Gets all object IDs at the specified world coordinates.
     */
    public List<Integer> getObjects(short x, short y, byte z) {
        long packedCoord = packCoordinate(x, y, z);
        int index = Arrays.binarySearch(coordinates, packedCoord);

        if (index < 0) {
            return Collections.emptyList();
        }

        int startOffset = offsets[index];
        int endOffset = (index + 1 < offsets.length) ? offsets[index + 1] : objectIds.length;

        List<Integer> result = new ArrayList<>(endOffset - startOffset);
        for (int i = startOffset; i < endOffset; i++) {
            result.add(objectIds[i]);
        }

        return result;
    }

    public boolean hasObjects(short x, short y, byte z) {
        long packedCoord = packCoordinate(x, y, z);
        return Arrays.binarySearch(coordinates, packedCoord) >= 0;
    }

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
     * Loads compressed object map from InputStream.
     */
    public static ObjectMapCompressed load(InputStream inputStream) throws IOException {
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

            if (version != 2) {
                throw new IOException("Unsupported compressed format version: " + version);
            }

            // Read entry count
            int entryCount = VarInt.readVarInt(dis);

            if (entryCount < 0 || entryCount > 10_000_000) {
                throw new IOException("Invalid entry count: " + entryCount);
            }

            System.out.println("Loading compressed object map with " + entryCount + " coordinates...");

            coordinates = new long[entryCount];
            offsets = new int[entryCount];
            List<Integer> objectIdList = new ArrayList<>(entryCount * 3);

            long currentCoord = 0;
            int currentOffset = 0;

            for (int i = 0; i < entryCount; i++) {
                // Read delta-encoded coordinate
                long delta = VarInt.readVarLong(dis);
                currentCoord += delta;
                coordinates[i] = currentCoord;

                // Read object count
                int objectCount = VarInt.readVarInt(dis);

                if (objectCount < 0 || objectCount > 1000) {
                    throw new IOException("Invalid object count at entry " + i + ": " + objectCount);
                }

                offsets[i] = currentOffset;

                // Read object IDs
                for (int j = 0; j < objectCount; j++) {
                    objectIdList.add(VarInt.readVarInt(dis));
                }

                currentOffset += objectCount;

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
        System.out.println("Loaded compressed object map: " + coordinates.length +
            " coordinates, " + objectIds.length + " objects in " + loadTime + "ms");

        return new ObjectMapCompressed(coordinates, offsets, objectIds, version);
    }

    /**
     * Loads compressed object map from file.
     */
    public static ObjectMapCompressed load(String filePath) throws IOException {
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            System.err.println("Object map file not found: " + filePath);
            return null;
        }

        return load(new FileInputStream(file));
    }
}
