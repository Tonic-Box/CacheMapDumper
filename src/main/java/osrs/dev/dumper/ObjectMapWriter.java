package osrs.dev.dumper;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Writes object IDs for each world point to a file.
 * Uses coordinate packing similar to CollisionMapWriter for consistency.
 */
public class ObjectMapWriter {
    private final Map<Long, List<Integer>> objectMap;

    public ObjectMapWriter() {
        this.objectMap = new ConcurrentHashMap<>();
    }

    /**
     * Adds an object ID at the specified world coordinates.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate (plane)
     * @param objectId The object ID to store
     */
    public void addObject(short x, short y, byte z, int objectId) {
        long packedCoord = packCoordinate(x, y, z);
        objectMap.computeIfAbsent(packedCoord, k -> new ArrayList<>()).add(objectId);
    }

    /**
     * Packs x, y, z coordinates into a single long value.
     * Uses same bit packing as collision map for consistency:
     * x: 13 bits, y: 15 bits, z: 4 bits
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
     * Saves the object map to the specified file path.
     * Coordinates are sorted for efficient binary search loading.
     *
     * @param filePath The file path to save to
     * @throws IOException If an I/O error occurs
     */
    public void save(String filePath) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();

        // Sort coordinates for binary search optimization
        List<Long> sortedCoords = new ArrayList<>(objectMap.keySet());
        sortedCoords.sort(Long::compareTo);

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file), 131072))) {

            // Write version marker for future compatibility
            dos.writeInt(1);

            // Write number of entries
            dos.writeInt(sortedCoords.size());

            // Write each entry in sorted order
            for (Long coord : sortedCoords) {
                List<Integer> objects = objectMap.get(coord);
                dos.writeLong(coord);
                dos.writeInt(objects.size());
                for (Integer objectId : objects) {
                    dos.writeInt(objectId);
                }
            }
        }
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
     * Gets the total number of object instances across all coordinates.
     *
     * @return The total object count
     */
    public int totalObjects() {
        return objectMap.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}
