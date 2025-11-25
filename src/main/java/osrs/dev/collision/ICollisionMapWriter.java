package osrs.dev.collision;

import java.io.IOException;

/**
 * Interface for writing collision map data.
 * Methods set pathability (true = pathable, false = blocked).
 */
public interface ICollisionMapWriter {

    /**
     * Sets north pathability for the given tile.
     * @param pathable true if north movement should be allowed
     */
    void setPathableNorth(int x, int y, int plane, boolean pathable);

    /**
     * Sets east pathability for the given tile.
     * @param pathable true if east movement should be allowed
     */
    void setPathableEast(int x, int y, int plane, boolean pathable);

    /**
     * Sets south pathability for the given tile.
     * Default implementation delegates to north of the tile below.
     * @param pathable true if south movement should be allowed
     */
    default void setPathableSouth(int x, int y, int plane, boolean pathable) {
        setPathableNorth(x, y - 1, plane, pathable);
    }

    /**
     * Sets west pathability for the given tile.
     * Default implementation delegates to east of the tile to the west.
     * @param pathable true if west movement should be allowed
     */
    default void setPathableWest(int x, int y, int plane, boolean pathable) {
        setPathableEast(x - 1, y, plane, pathable);
    }

    /**
     * Sets all four cardinal directions for a tile.
     * @param pathable true if movement should be allowed in all directions
     */
    default void setAllDirections(int x, int y, int plane, boolean pathable) {
        setPathableNorth(x, y, plane, pathable);
        setPathableEast(x, y, plane, pathable);
        setPathableSouth(x, y, plane, pathable);
        setPathableWest(x, y, plane, pathable);
    }

    /**
     * Saves the collision map to file.
     * @param filePath the path to save to
     * @throws IOException if an I/O error occurs
     */
    void save(String filePath) throws IOException;


    // ==================== Blocking-style convenience methods ====================
    // These provide backwards compatibility with the "blocking" API style
    // where blocking=true means cannot walk (opposite of pathable)

    /**
     * Sets north blocking state (blocking=true means cannot walk north).
     * Convenience method that inverts to pathable semantics.
     */
    default void northBlocking(int x, int y, int plane, boolean blocking) {
        setPathableNorth(x, y, plane, !blocking);
    }

    /**
     * Sets east blocking state (blocking=true means cannot walk east).
     */
    default void eastBlocking(int x, int y, int plane, boolean blocking) {
        setPathableEast(x, y, plane, !blocking);
    }

    /**
     * Sets south blocking state (blocking=true means cannot walk south).
     */
    default void southBlocking(int x, int y, int plane, boolean blocking) {
        setPathableSouth(x, y, plane, !blocking);
    }

    /**
     * Sets west blocking state (blocking=true means cannot walk west).
     */
    default void westBlocking(int x, int y, int plane, boolean blocking) {
        setPathableWest(x, y, plane, !blocking);
    }

    /**
     * Sets all four directions to blocking state.
     */
    default void fullBlocking(int x, int y, int plane, boolean blocking) {
        setAllDirections(x, y, plane, !blocking);
    }
}
