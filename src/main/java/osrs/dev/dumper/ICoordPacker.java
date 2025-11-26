package osrs.dev.dumper;

/**
 * Interface for coordinate packing strategies.
 * Allows different bit layouts for encoding x, y, plane coordinates into a single int.
 */
public interface ICoordPacker {
    /**
     * Flag bit for distinguishing east direction from north.
     * Applied at bit 30.
     */
    int E_FLAG = 1 << 30;

    /**
     * Packs x, y, plane coordinates into a single int for north direction.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param plane the plane/level
     * @return packed integer index
     */
    int pack(int x, int y, int plane);

    /**
     * Packs coordinates with E_FLAG for east direction.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param plane the plane/level
     * @return packed integer index with E_FLAG set
     */
    default int packEast(int x, int y, int plane) {
        return pack(x, y, plane) | E_FLAG;
    }

    /**
     * Extracts the X coordinate from a packed value.
     *
     * @param packed the packed integer (without E_FLAG)
     * @return the x coordinate
     */
    int unpackX(int packed);

    /**
     * Extracts the Y coordinate from a packed value.
     *
     * @param packed the packed integer (without E_FLAG)
     * @return the y coordinate
     */
    int unpackY(int packed);

    /**
     * Extracts the plane from a packed value.
     *
     * @param packed the packed integer (without E_FLAG)
     * @return the plane
     */
    int unpackPlane(int packed);
}
