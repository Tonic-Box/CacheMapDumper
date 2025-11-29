package osrs.dev.util;

import osrs.dev.Main;
import osrs.dev.reader.ObjectMapOptimized;

import java.util.List;

/**
 * Utility class for querying object IDs from the loaded ObjectMap.
 * Provides convenient methods to retrieve and display object information.
 */
public class ObjectMapQuery {

    /**
     * Gets all object IDs at the specified world point.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate (plane)
     * @return List of object IDs, or empty list if none exist
     */
    public static List<Integer> getObjectsAt(int x, int y, int z) {
        ObjectMapOptimized objectMap = Main.getObjectMap();
        if (objectMap == null) {
            System.err.println("Object map not loaded. Please update the object map first.");
            return List.of();
        }
        return objectMap.getObjects((short) x, (short) y, (byte) z);
    }

    /**
     * Checks if any objects exist at the specified coordinates.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate (plane)
     * @return True if objects exist at this location
     */
    public static boolean hasObjectsAt(int x, int y, int z) {
        ObjectMapOptimized objectMap = Main.getObjectMap();
        if (objectMap == null) {
            return false;
        }
        return objectMap.hasObjects((short) x, (short) y, (byte) z);
    }

    /**
     * Prints all object IDs at the specified coordinates to console.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate (plane)
     */
    public static void printObjectsAt(int x, int y, int z) {
        List<Integer> objects = getObjectsAt(x, y, z);
        if (objects.isEmpty()) {
            System.out.println("No objects at (" + x + ", " + y + ", " + z + ")");
        } else {
            System.out.println("Objects at (" + x + ", " + y + ", " + z + "):");
            for (int i = 0; i < objects.size(); i++) {
                System.out.println("  [" + i + "] ID: " + objects.get(i));
            }
        }
    }

    /**
     * Gets the total number of unique coordinates with objects.
     *
     * @return The number of coordinates, or 0 if map not loaded
     */
    public static int getTotalCoordinates() {
        ObjectMapOptimized objectMap = Main.getObjectMap();
        return objectMap != null ? objectMap.size() : 0;
    }

    /**
     * Checks if the object map is loaded and available.
     *
     * @return True if object map is loaded
     */
    public static boolean isLoaded() {
        return Main.getObjectMap() != null;
    }
}
