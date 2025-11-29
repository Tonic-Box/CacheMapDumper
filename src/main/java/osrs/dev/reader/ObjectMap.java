package osrs.dev.reader;

import java.io.*;
import java.util.*;

/**
 * Reads object ID data from a saved object map file.
 * Provides efficient lookup of object IDs at world coordinates.
 */
public class ObjectMap {
    private final Map<Long, List<Integer>> objectMap;
    private final int version;
    private static final List<Integer> EMPTY_LIST = Collections.emptyList();

    /**
     * Creates a new ObjectMap from the loaded data.
     *
     * @param objectMap The map of coordinates to object IDs
     * @param version The file format version
     */
    private ObjectMap(Map<Long, List<Integer>> objectMap, int version) {
        this.objectMap = objectMap;
        this.version = version;
    }

    /**
     * Gets all object IDs at the specified world coordinates.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate (plane)
     * @return List of object IDs at the coordinate, or empty list if none
     */
    public List<Integer> getObjects(short x, short y, byte z) {
        long packedCoord = packCoordinate(x, y, z);
        List<Integer> result = objectMap.get(packedCoord);
        return result != null ? result : EMPTY_LIST;
    }

    /**
     * Checks if there are any objects at the specified coordinates.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate (plane)
     * @return True if objects exist at this coordinate
     */
    public boolean hasObjects(short x, short y, byte z) {
        long packedCoord = packCoordinate(x, y, z);
        return objectMap.containsKey(packedCoord);
    }

    /**
     * Gets the number of unique coordinates with objects.
     *
     * @return The number of entries
     */
    public int size() {
        return objectMap.size();
    }

    /**
     * Gets the file format version.
     *
     * @return The version number
     */
    public int getVersion() {
        return version;
    }

    /**
     * Packs x, y, z coordinates into a single long value.
     * Uses same bit packing as collision map for consistency.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @return Packed coordinate as long
     */
    private long packCoordinate(short x, short y, byte z) {
        return (x & 8191) | ((long)(y & 32767) << 13) | ((long)(z & 15) << 28);
    }

    /**
     * Loads an object map from the specified file path.
     *
     * @param filePath The file path to load from
     * @return The loaded ObjectMap, or null if file doesn't exist
     * @throws IOException If an I/O error occurs
     */
    public static ObjectMap load(String filePath) throws IOException {
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            System.err.println("Object map file not found: " + filePath);
            return null;
        }

        long startTime = System.currentTimeMillis();
        int version;
        Map<Long, List<Integer>> objectMap;

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file), 65536))) {

            // Read version
            version = dis.readInt();

            // Read number of entries
            int entryCount = dis.readInt();

            // Pre-size HashMap with load factor to avoid rehashing
            objectMap = new HashMap<>((int)(entryCount / 0.75f) + 1);

            // Read each entry
            for (int i = 0; i < entryCount; i++) {
                long packedCoord = dis.readLong();
                int objectCount = dis.readInt();

                List<Integer> objects = new ArrayList<>(objectCount);
                for (int j = 0; j < objectCount; j++) {
                    objects.add(dis.readInt());
                }

                // Make list unmodifiable and compact
                objectMap.put(packedCoord, Collections.unmodifiableList(objects));
            }
        }

        long loadTime = System.currentTimeMillis() - startTime;
        System.out.println("Loaded object map: " + objectMap.size() +
            " unique coordinates in " + loadTime + "ms");
        return new ObjectMap(objectMap, version);
    }
}
