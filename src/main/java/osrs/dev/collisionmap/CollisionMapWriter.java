package osrs.dev.collisionmap;

import osrs.dev.tiledatamap.ITileDataMapWriter;

import java.io.IOException;

/**
 * Generic collision map writer backed by any ITileDataMapWriter implementation.
 * Maps NORTH and EAST direction bits with inverted semantics.
 *
 * pathable=false → set bit (blocked)
 * pathable=true → clear bit (not blocked)
 */
public class CollisionMapWriter {
    private final ITileDataMapWriter dataMapWriter;

    public CollisionMapWriter(ITileDataMapWriter dataMapWriter) {
        this.dataMapWriter = dataMapWriter;
    }

    public void setPathableNorth(int x, int y, int plane, boolean pathable) {
        if (!pathable) {
            dataMapWriter.setDataBit(x, y, plane, CollisionMap.NORTH_DATA_BIT_POS);
        }
    }

    public void setPathableEast(int x, int y, int plane, boolean pathable) {
        if (!pathable) {
            dataMapWriter.setDataBit(x, y, plane, CollisionMap.EAST_DATA_BIT_POS);
        }
    }

    public void setPathableSouth(int x, int y, int plane, boolean pathable) {
        setPathableNorth(x, y - 1, plane, pathable);
    }

    public void setPathableWest(int x, int y, int plane, boolean pathable) {
        setPathableEast(x - 1, y, plane, pathable);
    }

    public void northBlocking(int x, int y, int plane, boolean blocking) {
        setPathableNorth(x, y, plane, !blocking);
    }

    public void eastBlocking(int x, int y, int plane, boolean blocking) {
        setPathableEast(x, y, plane, !blocking);
    }

    public void southBlocking(int x, int y, int plane, boolean blocking) {
        setPathableSouth(x, y, plane, !blocking);
    }

    public void westBlocking(int x, int y, int plane, boolean blocking) {
        setPathableWest(x, y, plane, !blocking);
    }

    public void setPathableAllDirections(int x, int y, int plane, boolean pathable) {
        setPathableNorth(x, y, plane, pathable);
        setPathableEast(x, y, plane, pathable);
        setPathableSouth(x, y, plane, pathable);
        setPathableWest(x, y, plane, pathable);
    }

    public void fullBlocking(int x, int y, int plane, boolean blocking) {
        setPathableAllDirections(x, y, plane, !blocking);
    }

    public void save(String filePath) throws IOException {
        dataMapWriter.save(filePath);
    }
}
